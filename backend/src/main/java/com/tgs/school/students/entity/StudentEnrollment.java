package com.tgs.school.students.entity;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.entity.Section;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_year_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentEnrollment {
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
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "roll_number", length = 20)
    private String rollNumber;

    @Column(name = "enrollment_status", nullable = false, length = 30)
    @Builder.Default
    private String enrollmentStatus = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_from_enrollment_id")
    private StudentEnrollment promotedFromEnrollment;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "enrolled_at", nullable = false)
    @Builder.Default
    private LocalDate enrolledAt = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
