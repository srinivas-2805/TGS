package com.tgs.school.fees.entity;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_fees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "concession_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal concessionAmount = BigDecimal.ZERO;

    @Column(name = "scholarship_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal scholarshipAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "concession_reason")
    private String concessionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }

    public void recalculate() {
        BigDecimal net = totalAmount.subtract(concessionAmount).subtract(scholarshipAmount);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        this.netAmount = net;
        BigDecimal bal = net.subtract(paidAmount);
        if (bal.compareTo(BigDecimal.ZERO) < 0) bal = BigDecimal.ZERO;
        this.balanceAmount = bal;
        if (net.compareTo(BigDecimal.ZERO) == 0) {
            this.status = "WAIVED";
        } else if (bal.compareTo(BigDecimal.ZERO) == 0) {
            this.status = "PAID";
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = "PARTIAL";
        } else {
            this.status = "PENDING";
        }
    }
}
