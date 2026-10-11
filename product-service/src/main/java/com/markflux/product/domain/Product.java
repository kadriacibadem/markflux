package com.markflux.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Product(String sku, String name, String description, BigDecimal price, UUID categoryId,
            ProductStatus status) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(String name, String description, BigDecimal price, UUID categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.updatedAt = Instant.now();
    }

    public boolean changeStatus(ProductStatus newStatus) {
        if (status == newStatus) {
            return false;
        }
        status = newStatus;
        updatedAt = Instant.now();
        return true;
    }
}
