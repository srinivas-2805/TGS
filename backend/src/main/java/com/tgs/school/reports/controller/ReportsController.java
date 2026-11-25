package com.tgs.school.reports.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.reports.service.ReportsService;
import com.tgs.school.reports.service.ReportsService.DashboardSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT','TEACHER','LIBRARIAN')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> dashboard(@RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getDashboardSummary(academicYearId)));
    }

    @GetMapping("/students/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<byte[]> exportStudents(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId) {
        return buildExportResponse("TGS-Students-Report", format,
                reportsService.exportStudentsReport(format, academicYearId, classId, sectionId));
    }

    @GetMapping("/attendance/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<byte[]> exportAttendance(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return buildExportResponse("TGS-Attendance-Report", format,
                reportsService.exportAttendanceReport(format, academicYearId, classId, sectionId, from, to));
    }

    @GetMapping("/teachers-attendance/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<byte[]> exportTeacherAttendance(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return buildExportResponse("TGS-Teacher-Attendance-Report", format,
                reportsService.exportTeacherAttendanceReport(format, from, to));
    }

    @GetMapping("/fees/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> exportFees(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String status) {
        return buildExportResponse("TGS-Fee-Report", format,
                reportsService.exportFeeReport(format, academicYearId, status));
    }

    @GetMapping("/examinations/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<byte[]> exportExams(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam Long examId,
            @RequestParam Long classId) {
        return buildExportResponse("TGS-Exam-Report", format,
                reportsService.exportExamReport(format, examId, classId));
    }

    @GetMapping("/library/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','LIBRARIAN')")
    public ResponseEntity<byte[]> exportLibrary(@RequestParam(defaultValue = "pdf") String format) {
        return buildExportResponse("TGS-Library-Report", format, reportsService.exportLibraryReport(format));
    }

    @GetMapping("/transport/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> exportTransport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long routeId) {
        return buildExportResponse("TGS-Transport-Report", format, reportsService.exportTransportReport(format, routeId));
    }

    private ResponseEntity<byte[]> buildExportResponse(String baseName, String format, byte[] bytes) {
        boolean isExcel = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format);
        String ext = isExcel ? ".xlsx" : ".pdf";
        MediaType mediaType = isExcel
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + baseName + ext)
                .body(bytes);
    }
}
