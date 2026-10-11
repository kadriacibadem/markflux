package com.markflux.inventory.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "stock_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "stock_reservation_line", joinColumns = @JoinColumn(name = "reservation_id"))
    private List<ReservationLine> lines = new ArrayList<>();

    @Column(name = "expires_at")
    private Instant expiresAt;

    private String reason;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private StockReservation(UUID orderId, ReservationStatus status, List<ReservationLine> lines, Instant expiresAt,
            String reason) {
        this.orderId = orderId;
        this.status = status;
        this.lines = new ArrayList<>(lines);
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public static StockReservation reserved(UUID orderId, List<ReservationLine> lines, Instant expiresAt) {
        return new StockReservation(orderId, ReservationStatus.RESERVED, lines, expiresAt, null);
    }

    public static StockReservation rejected(UUID orderId, List<ReservationLine> lines, String reason) {
        return new StockReservation(orderId, ReservationStatus.REJECTED, lines, null, reason);
    }

    public static StockReservation releasedBeforeReservation(UUID orderId, String reason) {
        return new StockReservation(orderId, ReservationStatus.RELEASED, List.of(), null, reason);
    }

    public void confirm() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only a RESERVED reservation can be confirmed, status: " + status);
        }
        status = ReservationStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    public void release(String releaseReason) {
        if (!isActive()) {
            throw new IllegalStateException("Only an active reservation can be released, status: " + status);
        }
        status = ReservationStatus.RELEASED;
        reason = releaseReason;
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == ReservationStatus.RESERVED || status == ReservationStatus.CONFIRMED;
    }

    public List<ReservationLine> getLines() {
        return List.copyOf(lines);
    }
}
