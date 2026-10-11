package com.markflux.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "stock_movement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Column(name = "reference_id")
    private String referenceId;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StockMovement(UUID productId, int quantityChange, MovementType type, String referenceId, String reason) {
        this.productId = productId;
        this.quantityChange = quantityChange;
        this.type = type;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
