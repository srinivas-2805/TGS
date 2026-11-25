package com.tgs.school.academics.repository;

import com.tgs.school.academics.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {
    @Query("SELECT cs FROM ClassSubject cs JOIN FETCH cs.subject JOIN FETCH cs.schoolClass WHERE cs.schoolClass.id = :classId ORDER BY cs.subject.name")
    List<ClassSubject> findByClassIdWithDetails(@Param("classId") Long classId);

    Optional<ClassSubject> findBySchoolClassIdAndSubjectId(Long classId, Long subjectId);
}
