package com.tgs.school.administration.repository;

import com.tgs.school.administration.entity.WebsiteContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsiteContentRepository extends JpaRepository<WebsiteContent, Long> {
    Optional<WebsiteContent> findBySectionKey(String sectionKey);
    List<WebsiteContent> findByIsPublishedTrueOrderBySectionKeyAsc();
    List<WebsiteContent> findAllByOrderBySectionKeyAsc();
}
