package com.markflux.cart.repository;

import com.markflux.cart.domain.CartItem;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Modifying
    @Query("""
            update CartItem i
            set i.productName = :name, i.unitPrice = :price, i.available = :available
            where i.productId = :productId
            """)
    int updateProduct(@Param("productId") UUID productId, @Param("name") String name,
            @Param("price") BigDecimal price, @Param("available") boolean available);
}
