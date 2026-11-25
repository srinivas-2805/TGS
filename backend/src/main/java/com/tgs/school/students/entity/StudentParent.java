package com.tgs.school.students.entity;

import com.tgs.school.parents.entity.ParentProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "student_parents", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "parent_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentParent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentProfile parent;

    @Column(nullable = false, length = 30)
    private String relationship;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
