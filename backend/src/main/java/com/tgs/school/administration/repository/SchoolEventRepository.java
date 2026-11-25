package com.tgs.school.administration.repository;

import com.tgs.school.administration.entity.SchoolEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolEventRepository extends JpaRepository<SchoolEvent, Long> {
    List<SchoolEvent> findByIsPublicTrueOrderByEventDateDesc();
    List<SchoolEvent> findAllByOrderByEventDateDesc();
}
