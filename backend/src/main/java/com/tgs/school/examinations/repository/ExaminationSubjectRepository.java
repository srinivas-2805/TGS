package com.tgs.school.examinations.repository;

import com.tgs.school.examinations.entity.ExaminationSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExaminationSubjectRepository extends JpaRepository<ExaminationSubject, Long> {
    @Query("SELECT es FROM ExaminationSubject es JOIN FETCH es.schoolClass JOIN FETCH es.subject JOIN FETCH es.examination " +
           "WHERE es.examination.id = :examId AND (:classId IS NULL OR es.schoolClass.id = :classId) " +
           "ORDER BY es.schoolClass.levelOrder, es.examDate, es.subject.name")
    List<ExaminationSubject> findByExamAndClass(@Param("examId") Long examId, @Param("classId") Long classId);

    Optional<ExaminationSubject> findByExaminationIdAndSchoolClassIdAndSubjectId(Long examId, Long classId, Long subjectId);
}
