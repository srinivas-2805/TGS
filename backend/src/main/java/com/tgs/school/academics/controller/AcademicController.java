package com.tgs.school.academics.controller;

import com.tgs.school.academics.service.AcademicService;
import com.tgs.school.academics.service.AcademicService.*;
import com.tgs.school.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    @GetMapping("/academic-years")
    public ResponseEntity<ApiResponse<List<AcademicYearDto>>> listAcademicYears() {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listAcademicYears()));
    }

    @PostMapping("/academic-years")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<AcademicYearDto>> createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Academic year created", academicService.createAcademicYear(req)));
    }

    @PatchMapping("/academic-years/{id}/set-current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<AcademicYearDto>> setCurrent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Current academic year updated", academicService.setCurrentAcademicYear(id)));
    }

    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<SchoolClassDto>>> listClasses() {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listClasses()));
    }

    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<SectionDto>>> listSections(@RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listSections(academicYearId)));
    }

    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<SectionDto>> createSection(@Valid @RequestBody CreateSectionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Section created", academicService.createSection(req)));
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<SectionDto>> updateSection(@PathVariable Long id, @RequestBody CreateSectionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Section updated", academicService.updateSection(id, req)));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> listSubjects() {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listSubjects()));
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(@Valid @RequestBody CreateSubjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subject created", academicService.createSubject(req)));
    }

    @GetMapping("/classes/{classId}/subjects")
    public ResponseEntity<ApiResponse<List<ClassSubjectDto>>> listClassSubjects(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listClassSubjects(classId)));
    }

    @PostMapping("/class-subjects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<ClassSubjectDto>> assignSubject(@Valid @RequestBody AssignClassSubjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subject assigned to class", academicService.assignSubjectToClass(req)));
    }
}
