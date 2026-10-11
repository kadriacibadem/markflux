package com.markflux.cart.repository;

import com.markflux.cart.domain.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.userId = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO cart (id, user_id, created_at, updated_at)
            VALUES (:id, :userId, now(), now())
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    void createIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId);
}
