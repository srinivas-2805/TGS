package com.tgs.school.students.entity;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.teachers.entity.Teacher;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "student_remarks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentRemark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "remark_type", nullable = false, length = 30)
    @Builder.Default
    private String remarkType = "ACADEMIC";

    @Column(name = "remark_text", nullable = false, columnDefinition = "TEXT")
    private String remarkText;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
