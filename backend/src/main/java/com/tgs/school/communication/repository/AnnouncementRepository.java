package com.tgs.school.communication.repository;

import com.tgs.school.communication.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findTop10ByIsPublicTrueOrderByPublishedAtDesc();

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.targetClass LEFT JOIN FETCH a.targetSection LEFT JOIN FETCH a.publishedBy ORDER BY a.publishedAt DESC")
    List<Announcement> findAllWithDetails();

    Page<Announcement> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
