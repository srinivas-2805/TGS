package com.tgs.school.timetable.repository;

import com.tgs.school.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {
    @Query("SELECT t FROM TimetableEntry t JOIN FETCH t.schoolClass JOIN FETCH t.section LEFT JOIN FETCH t.subject LEFT JOIN FETCH t.teacher " +
           "WHERE t.academicYear.id = :yearId AND t.section.id = :sectionId ORDER BY t.dayOfWeek, t.periodNumber")
    List<TimetableEntry> findByYearAndSection(@Param("yearId") Long yearId, @Param("sectionId") Long sectionId);

    @Query("SELECT t FROM TimetableEntry t JOIN FETCH t.schoolClass JOIN FETCH t.section LEFT JOIN FETCH t.subject LEFT JOIN FETCH t.teacher " +
           "WHERE t.academicYear.id = :yearId AND t.teacher.id = :teacherId ORDER BY t.dayOfWeek, t.periodNumber")
    List<TimetableEntry> findByYearAndTeacher(@Param("yearId") Long yearId, @Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(t) > 0 FROM TimetableEntry t WHERE t.academicYear.id = :yearId AND t.dayOfWeek = :day " +
           "AND t.teacher.id = :teacherId AND (:excludeId IS NULL OR t.id <> :excludeId) " +
           "AND t.startTime < :endTime AND t.endTime > :startTime")
    boolean existsTeacherConflict(
            @Param("yearId") Long yearId,
            @Param("day") String day,
            @Param("teacherId") Long teacherId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(t) > 0 FROM TimetableEntry t WHERE t.academicYear.id = :yearId AND t.dayOfWeek = :day " +
           "AND t.roomNumber = :roomNumber AND (:excludeId IS NULL OR t.id <> :excludeId) " +
           "AND t.startTime < :endTime AND t.endTime > :startTime")
    boolean existsRoomConflict(
            @Param("yearId") Long yearId,
            @Param("day") String day,
            @Param("roomNumber") String roomNumber,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}
