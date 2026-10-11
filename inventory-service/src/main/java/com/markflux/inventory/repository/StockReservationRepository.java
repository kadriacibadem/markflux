package com.markflux.inventory.repository;

import com.markflux.inventory.domain.StockReservation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReservation r where r.orderId = :orderId")
    Optional<StockReservation> findByOrderIdForUpdate(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReservation r where r.id = :id")
    Optional<StockReservation> findByIdForUpdate(UUID id);

    @Query(value = """
            SELECT * FROM stock_reservation
            WHERE status = 'RESERVED' AND expires_at < :now
            ORDER BY expires_at
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<StockReservation> findExpiredReservations(Instant now);
}
