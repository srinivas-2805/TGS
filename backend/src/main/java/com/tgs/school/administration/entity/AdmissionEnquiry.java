package com.tgs.school.administration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "admission_enquiries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdmissionEnquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false, length = 150)
    private String studentName;

    @Column(name = "parent_name", nullable = false, length = 150)
    private String parentName;

    @Column(nullable = false, length = 20)
    private String phone;

    private String email;

    @Column(name = "applying_class_name", nullable = false, length = 40)
    private String applyingClassName;

    @Column(name = "previous_school", length = 200)
    private String previousSchool;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "NEW";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
