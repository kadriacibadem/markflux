package com.markflux.inventory.web;

import com.markflux.common.error.BusinessException;
import com.markflux.common.error.ConflictException;
import com.markflux.inventory.domain.ReservationLine;
import com.markflux.inventory.service.ReservationResult;
import com.markflux.inventory.service.StockReservationService;
import com.markflux.inventory.web.dto.CreateReservationRequest;
import com.markflux.inventory.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final StockReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody CreateReservationRequest request) {
        List<ReservationLine> lines = request.items().stream()
                .map(item -> new ReservationLine(item.productId(), item.quantity()))
                .toList();
        ReservationResult result = reservationService.reserveManually(request.orderId(), lines);
        return switch (result) {
            case ReservationResult.Reserved reserved -> ResponseEntity
                    .created(URI.create("/api/inventory/reservations/" + reserved.reservation().getId()))
                    .body(ReservationResponse.from(reserved.reservation()));
            case ReservationResult.Rejected rejected -> throw new BusinessException("INSUFFICIENT_STOCK",
                    "Not enough stock", Map.of("shortages", rejected.shortages()));
            case ReservationResult.AlreadyExists existing -> throw new ConflictException("RESERVATION_ALREADY_EXISTS",
                    "Order already has a reservation: " + existing.reservation().getId());
        };
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse getReservation(@PathVariable UUID reservationId) {
        return ReservationResponse.from(reservationService.getReservation(reservationId));
    }

    @PostMapping("/{reservationId}/confirm")
    public ReservationResponse confirm(@PathVariable UUID reservationId) {
        return ReservationResponse.from(reservationService.confirmById(reservationId));
    }

    @PostMapping("/{reservationId}/release")
    public ReservationResponse release(@PathVariable UUID reservationId) {
        return ReservationResponse.from(reservationService.releaseById(reservationId));
    }
}
