package com.tgs.school.academics.repository;

import com.tgs.school.academics.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByIsCurrentTrue();
    Optional<AcademicYear> findByName(String name);
    List<AcademicYear> findAllByOrderByStartDateDesc();
}
