package com.tgs.school.attendance.controller;

import com.tgs.school.attendance.service.LeaveService;
import com.tgs.school.attendance.service.LeaveService.*;
import com.tgs.school.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.listAll()));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> listForStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.listForStudent(studentId)));
    }

    @GetMapping("/teacher/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> myTeacherLeaves() {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.myTeacherLeaves()));
    }

    @PostMapping("/student")
    @PreAuthorize("hasAnyRole('PARENT','STUDENT','ADMIN','PRINCIPAL','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> applyStudentLeave(@Valid @RequestBody ApplyLeaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Leave request submitted", leaveService.applyStudentLeave(req)));
    }

    @PostMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> applyTeacherLeave(@Valid @RequestBody ApplyLeaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Teacher leave request submitted", leaveService.applyTeacherLeave(req)));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> reviewLeave(
            @PathVariable Long id,
            @Valid @RequestBody ReviewLeaveRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Leave request reviewed", leaveService.reviewLeave(id, req)));
    }
}
