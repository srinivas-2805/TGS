package com.tgs.school.administration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgs.school.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "website_content")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_key", nullable = false, unique = true, length = 80)
    private String sectionKey;

    @Column(nullable = false, length = 200)
    private String title;

    private String subtitle;

    @Column(name = "body_content", nullable = false, columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }
}
