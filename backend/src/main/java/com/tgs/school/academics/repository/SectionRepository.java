package com.tgs.school.academics.repository;

import com.tgs.school.academics.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    @Query("SELECT s FROM Section s JOIN FETCH s.schoolClass JOIN FETCH s.academicYear LEFT JOIN FETCH s.classTeacher WHERE s.academicYear.id = :yearId ORDER BY s.schoolClass.levelOrder, s.name")
    List<Section> findByAcademicYearIdWithDetails(@Param("yearId") Long yearId);

    @Query("SELECT s FROM Section s JOIN FETCH s.schoolClass JOIN FETCH s.academicYear LEFT JOIN FETCH s.classTeacher WHERE s.classTeacher.id = :teacherId AND s.academicYear.id = :yearId")
    List<Section> findByClassTeacherIdAndYear(@Param("teacherId") Long teacherId, @Param("yearId") Long yearId);

    Optional<Section> findBySchoolClassIdAndAcademicYearIdAndName(Long classId, Long academicYearId, String name);
}
