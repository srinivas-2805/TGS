package com.tgs.school.examinations.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.examinations.entity.GradingRule;
import com.tgs.school.examinations.service.ExaminationService;
import com.tgs.school.examinations.service.ExaminationService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamDto>>> list(@RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationService.listExaminations(academicYearId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<ExamDto>> create(@Valid @RequestBody CreateExamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Examination created", examinationService.createExamination(req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<ExamDto>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Examination status updated",
                examinationService.updateExamStatus(id, body.getOrDefault("status", "PUBLISHED"))));
    }

    @GetMapping("/{examId}/subjects")
    public ResponseEntity<ApiResponse<List<ExamSubjectDto>>> listSubjects(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationService.listExamSubjects(examId, classId)));
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<ExamSubjectDto>> configureSubject(@Valid @RequestBody ConfigureExamSubjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Examination subject configured", examinationService.configureExamSubject(req)));
    }

    @PostMapping("/marks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<MarkDto>>> enterMarks(@Valid @RequestBody EnterMarksRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Marks saved", examinationService.enterMarks(req)));
    }

    @GetMapping("/subjects/{examSubjectId}/marks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<MarkDto>>> getSubjectMarks(@PathVariable Long examSubjectId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationService.getMarksForExamSubject(examSubjectId)));
    }

    @GetMapping("/report-card")
    public ResponseEntity<ApiResponse<ReportCardDto>> getReportCard(
            @RequestParam Long studentId,
            @RequestParam Long examinationId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationService.getStudentReportCard(studentId, examinationId)));
    }

    @GetMapping("/report-card/pdf")
    public ResponseEntity<byte[]> downloadReportCardPdf(
            @RequestParam Long studentId,
            @RequestParam Long examinationId) {
        byte[] pdf = examinationService.exportReportCardPdf(studentId, examinationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=TGS-ReportCard-" + studentId + "-" + examinationId + ".pdf")
                .body(pdf);
    }

    @GetMapping("/{examId}/class-performance/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<MarkDto>>> classPerformance(
            @PathVariable Long examId,
            @PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(examinationService.getClassPerformance(examId, classId)));
    }

    @GetMapping("/grading-rules")
    public ResponseEntity<ApiResponse<List<GradingRule>>> gradingRules() {
        return ResponseEntity.ok(ApiResponse.ok(examinationService.listGradingRules()));
    }

    @PostMapping("/grading-rules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<GradingRule>> createGradingRule(@Valid @RequestBody SaveGradingRuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Grading rule saved", examinationService.createGradingRule(req)));
    }

    @DeleteMapping("/grading-rules/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGradingRule(@PathVariable Long id) {
        examinationService.deleteGradingRule(id);
        return ResponseEntity.ok(ApiResponse.okMessage("Grading rule removed"));
    }
}
