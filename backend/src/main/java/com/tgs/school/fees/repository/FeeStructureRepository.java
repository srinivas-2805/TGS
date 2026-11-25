package com.tgs.school.fees.repository;

import com.tgs.school.fees.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    @Query("SELECT fs FROM FeeStructure fs JOIN FETCH fs.academicYear JOIN FETCH fs.schoolClass " +
           "WHERE (:yearId IS NULL OR fs.academicYear.id = :yearId) AND (:classId IS NULL OR fs.schoolClass.id = :classId) " +
           "ORDER BY fs.schoolClass.levelOrder, fs.name")
    List<FeeStructure> findFiltered(@Param("yearId") Long yearId, @Param("classId") Long classId);
}
