package com.tgs.school.homework.repository;

import com.tgs.school.homework.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    @Query("SELECT sm FROM StudyMaterial sm JOIN FETCH sm.schoolClass LEFT JOIN FETCH sm.section JOIN FETCH sm.subject JOIN FETCH sm.teacher " +
           "WHERE (:classId IS NULL OR sm.schoolClass.id = :classId) " +
           "AND (:sectionId IS NULL OR sm.section IS NULL OR sm.section.id = :sectionId) " +
           "ORDER BY sm.createdAt DESC")
    List<StudyMaterial> findFiltered(@Param("classId") Long classId, @Param("sectionId") Long sectionId);
}
