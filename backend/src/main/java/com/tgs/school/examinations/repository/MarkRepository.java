package com.tgs.school.examinations.repository;

import com.tgs.school.examinations.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {
    Optional<Mark> findByExaminationSubjectIdAndStudentId(Long examSubjectId, Long studentId);

    @Query("SELECT m FROM Mark m JOIN FETCH m.examinationSubject es JOIN FETCH es.subject JOIN FETCH es.examination " +
           "WHERE m.student.id = :studentId AND (:examId IS NULL OR es.examination.id = :examId) " +
           "ORDER BY es.examination.startDate DESC, es.subject.name")
    List<Mark> findByStudentAndExam(@Param("studentId") Long studentId, @Param("examId") Long examId);

    @Query("SELECT m FROM Mark m JOIN FETCH m.student JOIN FETCH m.enrollment JOIN FETCH m.examinationSubject es JOIN FETCH es.subject " +
           "WHERE es.examination.id = :examId AND es.schoolClass.id = :classId " +
           "ORDER BY m.student.fullName, es.subject.name")
    List<Mark> findByExamAndClass(@Param("examId") Long examId, @Param("classId") Long classId);

    @Query("SELECT m FROM Mark m JOIN FETCH m.student JOIN FETCH m.enrollment WHERE m.examinationSubject.id = :examSubjectId ORDER BY m.student.fullName")
    List<Mark> findByExamSubject(@Param("examSubjectId") Long examSubjectId);
}
