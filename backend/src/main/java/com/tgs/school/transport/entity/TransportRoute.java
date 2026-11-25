package com.tgs.school.transport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "transport_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransportRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(name = "route_code", nullable = false, unique = true, length = 40)
    private String routeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "pickup_points_json", nullable = false, columnDefinition = "TEXT")
    private String pickupPointsJson;

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyFee = BigDecimal.ZERO;

    @Column(name = "morning_start_time")
    private LocalTime morningStartTime;

    @Column(name = "afternoon_departure_time")
    private LocalTime afternoonDepartureTime;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
