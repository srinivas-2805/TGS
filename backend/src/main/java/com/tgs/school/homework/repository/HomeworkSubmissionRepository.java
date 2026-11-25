package com.tgs.school.homework.repository;

import com.tgs.school.homework.entity.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {
    Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    @Query("SELECT hs FROM HomeworkSubmission hs JOIN FETCH hs.student WHERE hs.homework.id = :homeworkId ORDER BY hs.submittedAt DESC")
    List<HomeworkSubmission> findByHomeworkIdWithStudent(@Param("homeworkId") Long homeworkId);

    @Query("SELECT hs FROM HomeworkSubmission hs JOIN FETCH hs.homework WHERE hs.student.id = :studentId ORDER BY hs.submittedAt DESC")
    List<HomeworkSubmission> findByStudentIdWithHomework(@Param("studentId") Long studentId);
}
