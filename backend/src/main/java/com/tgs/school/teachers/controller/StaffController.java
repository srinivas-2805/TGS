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

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
public class StaffController {

    private final TeacherService teacherService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StaffDto>>> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(teacherService.listStaff(PageRequest.of(page, size, Sort.by("fullName")))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffDto>> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Staff member created", teacherService.createStaff(req)));
    }
}
