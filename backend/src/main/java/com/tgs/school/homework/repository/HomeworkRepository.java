package com.tgs.school.homework.repository;

import com.tgs.school.homework.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    @Query("SELECT h FROM Homework h JOIN FETCH h.schoolClass JOIN FETCH h.section JOIN FETCH h.subject JOIN FETCH h.teacher " +
           "WHERE (:yearId IS NULL OR h.academicYear.id = :yearId) " +
           "AND (:classId IS NULL OR h.schoolClass.id = :classId) " +
           "AND (:sectionId IS NULL OR h.section.id = :sectionId) " +
           "AND (:teacherId IS NULL OR h.teacher.id = :teacherId) " +
           "ORDER BY h.dueDate DESC, h.createdAt DESC")
    List<Homework> findFiltered(
            @Param("yearId") Long yearId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("teacherId") Long teacherId);
}
