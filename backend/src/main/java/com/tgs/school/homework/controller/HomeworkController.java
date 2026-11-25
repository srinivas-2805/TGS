package com.tgs.school.homework.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.homework.service.HomeworkService;
import com.tgs.school.homework.service.HomeworkService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HomeworkDto>>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(ApiResponse.ok(homeworkService.listHomework(classId, sectionId, teacherId)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<HomeworkDto>>> listForStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(homeworkService.listForStudent(studentId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<HomeworkDto>> createJson(@Valid @RequestBody CreateHomeworkRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Homework created", homeworkService.createHomework(req, null)));
    }

    @PostMapping(value = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<HomeworkDto>> createWithFile(
            @RequestParam Long sectionId,
            @RequestParam Long subjectId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(defaultValue = "true") boolean allowSubmission,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CreateHomeworkRequest req = new CreateHomeworkRequest();
        req.setSectionId(sectionId);
        req.setSubjectId(subjectId);
        req.setTitle(title);
        req.setDescription(description);
        req.setDueDate(dueDate);
        req.setAllowSubmission(allowSubmission);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Homework created", homeworkService.createHomework(req, file)));
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionDto>> submit(
            @PathVariable Long id,
            @RequestParam(required = false) String submissionText,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Homework submitted", homeworkService.submitHomework(id, submissionText, file)));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<SubmissionDto>>> submissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(homeworkService.listSubmissions(id)));
    }

    @PatchMapping("/submissions/{submissionId}/review")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SubmissionDto>> review(
            @PathVariable Long submissionId,
            @Valid @RequestBody ReviewSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Submission reviewed", homeworkService.reviewSubmission(submissionId, req)));
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        FileDownload dl = homeworkService.downloadHomeworkAttachment(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dl.fileName() + "\"")
                .body(dl.resource());
    }

    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<StudyMaterialDto>>> listMaterials(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.ok(homeworkService.listMaterials(classId, sectionId)));
    }

    @PostMapping(value = "/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudyMaterialDto>> uploadMaterial(
            @RequestParam Long sectionId,
            @RequestParam Long subjectId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "NOTES") String materialType,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Study material uploaded",
                        homeworkService.uploadStudyMaterial(sectionId, subjectId, title, description, materialType, file)));
    }

    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id) {
        FileDownload dl = homeworkService.downloadMaterial(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dl.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dl.fileName() + "\"")
                .body(dl.resource());
    }
}
