package com.tgs.school.administration.repository;

import com.tgs.school.administration.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GalleryRepository extends JpaRepository<GalleryItem, Long> {
    List<GalleryItem> findByIsPublishedTrueOrderByDisplayOrderAscCreatedAtDesc();
    List<GalleryItem> findAllByOrderByDisplayOrderAscCreatedAtDesc();
}
