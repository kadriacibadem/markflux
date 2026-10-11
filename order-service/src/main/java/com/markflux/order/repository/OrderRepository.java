package com.markflux.order.repository;

import com.markflux.order.domain.Order;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    @Query(value = """
            SELECT * FROM orders
            WHERE status = 'PENDING' AND created_at < :createdBefore
            ORDER BY created_at
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Order> findPendingOrdersCreatedBefore(Instant createdBefore);
}
