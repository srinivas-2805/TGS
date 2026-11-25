package com.tgs.school.teachers.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.teachers.service.TeacherService;
import com.tgs.school.teachers.service.TeacherService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER','LIBRARIAN')")
    public ResponseEntity<ApiResponse<PageResponse<TeacherDto>>> listTeachers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                teacherService.listTeachers(q, PageRequest.of(page, size, Sort.by("fullName")))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<TeacherDto>> createTeacher(@Valid @RequestBody CreateTeacherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Teacher created", teacherService.createTeacher(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<TeacherDto>> updateTeacher(@PathVariable Long id, @Valid @RequestBody CreateTeacherRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Teacher updated", teacherService.updateTeacher(id, req)));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentDto>>> listAssignments(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(ApiResponse.ok(teacherService.listAssignments(academicYearId, teacherId)));
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentDto>>> myAssignments() {
        return ResponseEntity.ok(ApiResponse.ok(teacherService.myAssignments()));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<TeacherAssignmentDto>> assignTeacher(@Valid @RequestBody AssignTeacherRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Teacher assigned", teacherService.assignTeacher(req)));
    }
}
