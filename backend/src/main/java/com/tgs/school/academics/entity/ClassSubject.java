package com.tgs.school.academics.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_subjects", uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "max_marks", nullable = false)
    @Builder.Default
    private Integer maxMarks = 100;

    @Column(name = "passing_marks", nullable = false)
    @Builder.Default
    private Integer passingMarks = 35;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private boolean isMandatory = true;
}
