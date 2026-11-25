package com.tgs.school.examinations.repository;

import com.tgs.school.examinations.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    @Query("SELECT e FROM Examination e JOIN FETCH e.academicYear WHERE (:yearId IS NULL OR e.academicYear.id = :yearId) ORDER BY e.startDate DESC")
    List<Examination> findByYear(@Param("yearId") Long yearId);

    long countByStatusIn(List<String> statuses);
}
