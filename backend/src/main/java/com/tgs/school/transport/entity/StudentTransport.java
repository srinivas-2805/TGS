package com.tgs.school.transport.entity;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.students.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_transport", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_year_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentTransport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private TransportRoute route;

    @Column(name = "pickup_point", nullable = false, length = 150)
    private String pickupPoint;

    @Column(name = "drop_point", nullable = false, length = 150)
    private String dropPoint;

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyFee = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDate assignedAt = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
