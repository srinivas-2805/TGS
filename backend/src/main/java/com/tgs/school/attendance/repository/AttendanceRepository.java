package com.tgs.school.attendance.repository;

import com.tgs.school.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByStudentIdAndAttendanceDateAndSession(Long studentId, LocalDate date, String session);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student JOIN FETCH a.schoolClass JOIN FETCH a.section " +
           "WHERE a.section.id = :sectionId AND a.attendanceDate = :date AND a.session = :session ORDER BY a.student.fullName")
    List<Attendance> findBySectionAndDateAndSession(
            @Param("sectionId") Long sectionId,
            @Param("date") LocalDate date,
            @Param("session") String session);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.schoolClass JOIN FETCH a.section " +
           "WHERE a.student.id = :studentId AND (:yearId IS NULL OR a.academicYear.id = :yearId) " +
           "ORDER BY a.attendanceDate DESC")
    List<Attendance> findByStudentAndYear(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student JOIN FETCH a.schoolClass JOIN FETCH a.section " +
           "WHERE a.academicYear.id = :yearId " +
           "AND (:classId IS NULL OR a.schoolClass.id = :classId) " +
           "AND (:sectionId IS NULL OR a.section.id = :sectionId) " +
           "AND a.attendanceDate BETWEEN :from AND :to " +
           "ORDER BY a.attendanceDate DESC, a.student.fullName")
    List<Attendance> findReportRecords(
            @Param("yearId") Long yearId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    long countByAttendanceDateAndStatus(LocalDate date, String status);
    long countByAttendanceDate(LocalDate date);
}
