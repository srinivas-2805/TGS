package com.tgs.school.students.repository;

import com.tgs.school.students.entity.StudentRemark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRemarkRepository extends JpaRepository<StudentRemark, Long> {
    @Query("SELECT r FROM StudentRemark r LEFT JOIN FETCH r.teacher JOIN FETCH r.academicYear WHERE r.student.id = :studentId ORDER BY r.createdAt DESC")
    List<StudentRemark> findByStudentIdWithDetails(@Param("studentId") Long studentId);
}
