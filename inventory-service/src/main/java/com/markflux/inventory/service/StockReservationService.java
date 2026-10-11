package com.markflux.inventory.service;

import com.markflux.common.error.ConflictException;
import com.markflux.common.error.NotFoundException;
import com.markflux.inventory.domain.Inventory;
import com.markflux.inventory.domain.MovementType;
import com.markflux.inventory.domain.ReservationLine;
import com.markflux.inventory.domain.ReservationStatus;
import com.markflux.inventory.domain.StockMovement;
import com.markflux.inventory.domain.StockReservation;
import com.markflux.inventory.repository.InventoryRepository;
import com.markflux.inventory.repository.StockMovementRepository;
import com.markflux.inventory.repository.StockReservationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StockReservationService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementRepository movementRepository;
    private final Duration reservationTtl;

    public StockReservationService(InventoryRepository inventoryRepository,
            StockReservationRepository reservationRepository, StockMovementRepository movementRepository,
            @Value("${markflux.inventory.reservation-ttl:15m}") Duration reservationTtl) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.movementRepository = movementRepository;
        this.reservationTtl = reservationTtl;
    }

    @Transactional
    public ReservationResult reserveForOrder(UUID orderId, List<ReservationLine> requestedLines) {
        return reserve(orderId, requestedLines, null);
    }

    @Transactional
    public ReservationResult reserveManually(UUID orderId, List<ReservationLine> requestedLines) {
        return reserve(orderId, requestedLines, Instant.now().plus(reservationTtl));
    }

    @Transactional
    public int releaseExpiredReservations(Instant now) {
        List<StockReservation> expired = reservationRepository.findExpiredReservations(now);
        expired.forEach(reservation -> release(reservation, "Reservation expired"));
        return expired.size();
    }

    private ReservationResult reserve(UUID orderId, List<ReservationLine> requestedLines, Instant expiresAt) {
        Optional<StockReservation> existing = reservationRepository.findByOrderIdForUpdate(orderId);
        if (existing.isPresent()) {
            return new ReservationResult.AlreadyExists(existing.get());
        }

        List<ReservationLine> lines = ReservationLine.mergeAndSort(requestedLines);
        Map<UUID, Inventory> inventories = lockInventories(lines);

        List<StockShortage> shortages = new ArrayList<>();
        for (ReservationLine line : lines) {
            Inventory inventory = inventories.get(line.productId());
            if (inventory == null || !inventory.canReserve(line.quantity())) {
                int available = inventory == null ? 0 : inventory.availableQuantity();
                shortages.add(new StockShortage(line.productId(), line.quantity(), available));
            }
        }
        if (!shortages.isEmpty()) {
            reservationRepository.save(StockReservation.rejected(orderId, lines, "Insufficient stock"));
            log.info("Not enough stock for order {}: {}", orderId, shortages);
            return new ReservationResult.Rejected(shortages);
        }

        for (ReservationLine line : lines) {
            inventories.get(line.productId()).reserve(line.quantity());
        }
        StockReservation reservation = reservationRepository.save(StockReservation.reserved(orderId, lines, expiresAt));
        log.info("Stock reserved for order {}", orderId);
        return new ReservationResult.Reserved(reservation);
    }

    @Transactional
    public void confirmForOrder(UUID orderId) {
        reservationRepository.findByOrderIdForUpdate(orderId)
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .ifPresentOrElse(this::confirm, () -> log.warn("No reservation to confirm for order {}", orderId));
    }

    @Transactional
    public void releaseForOrder(UUID orderId, String reason) {
        Optional<StockReservation> reservation = reservationRepository.findByOrderIdForUpdate(orderId);
        if (reservation.isEmpty()) {
            reservationRepository.save(StockReservation.releasedBeforeReservation(orderId, reason));
            return;
        }
        if (reservation.get().isActive()) {
            release(reservation.get(), reason);
        }
    }

    @Transactional
    public StockReservation confirmById(UUID reservationId) {
        StockReservation reservation = findForUpdate(reservationId);
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new ConflictException("INVALID_RESERVATION_STATE",
                    "Reservation cannot be confirmed in status " + reservation.getStatus());
        }
        confirm(reservation);
        return reservation;
    }

    @Transactional
    public StockReservation releaseById(UUID reservationId) {
        StockReservation reservation = findForUpdate(reservationId);
        if (!reservation.isActive()) {
            throw new ConflictException("INVALID_RESERVATION_STATE",
                    "Reservation cannot be released in status " + reservation.getStatus());
        }
        release(reservation, "Released manually");
        return reservation;
    }

    @Transactional(readOnly = true)
    public StockReservation getReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow(() -> notFound(reservationId));
    }

    private void confirm(StockReservation reservation) {
        Map<UUID, Inventory> inventories = lockInventories(reservation.getLines());
        for (ReservationLine line : reservation.getLines()) {
            inventories.get(line.productId()).commitReservation(line.quantity());
            movementRepository.save(new StockMovement(line.productId(), -line.quantity(),
                    MovementType.ORDER_FULFILLMENT, reservation.getOrderId().toString(), "Order confirmed"));
        }
        reservation.confirm();
        log.info("Reservation confirmed for order {}", reservation.getOrderId());
    }

    private void release(StockReservation reservation, String reason) {
        boolean alreadySold = reservation.getStatus() == ReservationStatus.CONFIRMED;
        Map<UUID, Inventory> inventories = lockInventories(reservation.getLines());
        for (ReservationLine line : reservation.getLines()) {
            Inventory inventory = inventories.get(line.productId());
            if (alreadySold) {
                inventory.restock(line.quantity());
                movementRepository.save(new StockMovement(line.productId(), line.quantity(),
                        MovementType.ORDER_CANCELLATION, reservation.getOrderId().toString(), reason));
            } else {
                inventory.releaseReservation(line.quantity());
            }
        }
        reservation.release(reason);
        log.info("Reservation released for order {}", reservation.getOrderId());
    }

    private Map<UUID, Inventory> lockInventories(List<ReservationLine> lines) {
        List<UUID> productIds = lines.stream().map(ReservationLine::productId).sorted().toList();
        Map<UUID, Inventory> inventories = new HashMap<>();
        for (Inventory inventory : inventoryRepository.findAllForUpdate(productIds)) {
            inventories.put(inventory.getProductId(), inventory);
        }
        return inventories;
    }

    private StockReservation findForUpdate(UUID reservationId) {
        return reservationRepository.findByIdForUpdate(reservationId).orElseThrow(() -> notFound(reservationId));
    }

    private static NotFoundException notFound(UUID reservationId) {
        return new NotFoundException("RESERVATION_NOT_FOUND", "Reservation not found: " + reservationId);
    }
}
