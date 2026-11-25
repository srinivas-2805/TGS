package com.tgs.school.examinations.entity;

import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.entity.Subject;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "examination_subjects", uniqueConstraints = @UniqueConstraint(columnNames = {"examination_id", "class_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExaminationSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_marks", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal maxMarks = BigDecimal.valueOf(100);

    @Column(name = "passing_marks", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal passingMarks = BigDecimal.valueOf(35);

    @Column(name = "is_activity_based", nullable = false)
    @Builder.Default
    private boolean isActivityBased = false;

    @Column(name = "room_number", length = 30)
    private String roomNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
