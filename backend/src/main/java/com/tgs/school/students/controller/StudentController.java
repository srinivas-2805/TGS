package com.tgs.school.students.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.students.service.StudentService;
import com.tgs.school.students.service.StudentService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER','ACCOUNTANT','LIBRARIAN')")
    public ResponseEntity<ApiResponse<PageResponse<StudentSummaryDto>>> listStudents(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                studentService.listStudents(academicYearId, classId, sectionId, status, q,
                        PageRequest.of(page, size, Sort.by("id").descending()))));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentDetailDto>> myProfile() {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getMyProfile()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDetailDto>> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getStudentDetail(id)));
    }

    @PostMapping("/admit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<StudentDetailDto>> admitStudent(@Valid @RequestBody AdmitStudentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Student admitted successfully", studentService.admitStudent(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<StudentDetailDto>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Student updated", studentService.updateStudent(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        studentService.updateStudentStatus(id, body.getOrDefault("status", "ACTIVE"), body.get("reason"));
        return ResponseEntity.ok(ApiResponse.okMessage("Student status updated"));
    }

    @PostMapping("/promote")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentSummaryDto>>> promoteStudents(@Valid @RequestBody PromoteStudentsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Students promoted successfully", studentService.promoteStudents(req)));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<DocumentDto>> uploadDocument(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CERTIFICATE") String documentType,
            @RequestParam(required = false) String title,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Document uploaded", studentService.uploadDocument(id, documentType, title, file)));
    }

    @GetMapping("/{id}/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @PathVariable Long docId) {
        DocumentDownload download = studentService.downloadDocument(id, docId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.fileName() + "\"")
                .body(download.resource());
    }

    @GetMapping("/{id}/id-card/pdf")
    public ResponseEntity<byte[]> downloadIdCard(@PathVariable Long id) {
        byte[] pdf = studentService.generateIdCardPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=TGS-IDCard-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping("/{id}/remarks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<RemarkDto>> addRemark(
            @PathVariable Long id,
            @Valid @RequestBody AddRemarkRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Remark added", studentService.addRemark(id, req)));
    }
}
