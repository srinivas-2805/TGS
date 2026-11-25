package com.tgs.school.teachers.repository;

import com.tgs.school.teachers.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    @Query("SELECT ta FROM TeacherAssignment ta " +
           "JOIN FETCH ta.teacher JOIN FETCH ta.schoolClass JOIN FETCH ta.section JOIN FETCH ta.subject JOIN FETCH ta.academicYear " +
           "WHERE ta.teacher.id = :teacherId AND ta.academicYear.id = :yearId " +
           "ORDER BY ta.schoolClass.levelOrder, ta.section.name, ta.subject.name")
    List<TeacherAssignment> findByTeacherAndYear(@Param("teacherId") Long teacherId, @Param("yearId") Long yearId);

    @Query("SELECT ta FROM TeacherAssignment ta " +
           "JOIN FETCH ta.teacher JOIN FETCH ta.schoolClass JOIN FETCH ta.section JOIN FETCH ta.subject " +
           "WHERE ta.academicYear.id = :yearId ORDER BY ta.schoolClass.levelOrder, ta.section.name")
    List<TeacherAssignment> findAllByYear(@Param("yearId") Long yearId);

    Optional<TeacherAssignment> findByAcademicYearIdAndSectionIdAndSubjectId(Long academicYearId, Long sectionId, Long subjectId);

    boolean existsByTeacherIdAndSectionIdAndAcademicYearId(Long teacherId, Long sectionId, Long academicYearId);
    boolean existsByTeacherIdAndSectionIdAndSubjectIdAndAcademicYearId(Long teacherId, Long sectionId, Long subjectId, Long academicYearId);
}
