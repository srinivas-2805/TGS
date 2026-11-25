package com.tgs.school.attendance.repository;

import com.tgs.school.attendance.entity.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {
    Optional<TeacherAttendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate date);

    @Query("SELECT ta FROM TeacherAttendance ta JOIN FETCH ta.teacher WHERE ta.attendanceDate = :date ORDER BY ta.teacher.fullName")
    List<TeacherAttendance> findByDate(@Param("date") LocalDate date);

    @Query("SELECT ta FROM TeacherAttendance ta JOIN FETCH ta.teacher WHERE ta.attendanceDate BETWEEN :from AND :to ORDER BY ta.attendanceDate DESC")
    List<TeacherAttendance> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
