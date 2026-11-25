package com.tgs.school.attendance.repository;

import com.tgs.school.attendance.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @Query("SELECT lr FROM LeaveRequest lr LEFT JOIN FETCH lr.student LEFT JOIN FETCH lr.teacher LEFT JOIN FETCH lr.requestedBy ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findAllWithDetails();

    @Query("SELECT lr FROM LeaveRequest lr LEFT JOIN FETCH lr.student WHERE lr.student.id = :studentId ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT lr FROM LeaveRequest lr LEFT JOIN FETCH lr.teacher WHERE lr.teacher.id = :teacherId ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByTeacherId(@Param("teacherId") Long teacherId);
}
