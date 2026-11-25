package com.tgs.school.examinations.entity;

import com.tgs.school.academics.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "grading_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @Column(name = "grade_name", nullable = false, length = 10)
    private String gradeName;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(name = "grade_point", precision = 4, scale = 2)
    private BigDecimal gradePoint;

    private String description;

    @Column(name = "is_passing", nullable = false)
    @Builder.Default
    private boolean isPassing = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
