package com.tgs.school.attendance.controller;

import com.tgs.school.attendance.service.AttendanceService;
import com.tgs.school.attendance.service.AttendanceService.*;
import com.tgs.school.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordDto>>> markStudentAttendance(
            @Valid @RequestBody MarkStudentAttendanceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attendance saved", attendanceService.markStudentAttendance(req)));
    }

    @PatchMapping("/students/{id}/correct")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceRecordDto>> correctAttendance(
            @PathVariable Long id,
            @Valid @RequestBody CorrectAttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Attendance corrected", attendanceService.correctAttendance(id, req)));
    }

    @GetMapping("/section")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordDto>>> getSectionAttendance(
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "FULL_DAY") String session) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getSectionAttendance(sectionId, date, session)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<StudentAttendanceSummaryDto>> getStudentSummary(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getStudentAttendanceSummary(studentId, academicYearId)));
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordDto>>> getReport(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getMonthlyReport(academicYearId, classId, sectionId, from, to)));
    }

    @PostMapping("/teachers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherAttendanceDto>>> markTeacherAttendance(
            @Valid @RequestBody MarkTeacherAttendanceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Teacher attendance saved", attendanceService.markTeacherAttendance(req)));
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<List<TeacherAttendanceDto>>> getTeacherAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getTeacherAttendance(date, from, to)));
    }
}
