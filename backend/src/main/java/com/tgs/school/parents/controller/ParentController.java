package com.tgs.school.parents.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.parents.service.ParentService;
import com.tgs.school.parents.service.ParentService.*;
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
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ParentDto>>> listParents(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                parentService.listParents(q, PageRequest.of(page, size, Sort.by("fullName")))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<ParentDto>> createParent(@Valid @RequestBody CreateParentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parent created", parentService.createParent(req)));
    }

    @PostMapping("/link-student")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<ParentDto>> linkStudent(@Valid @RequestBody LinkStudentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Student linked to parent", parentService.linkStudent(req)));
    }

    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<ChildSummaryDto>>> myChildren() {
        return ResponseEntity.ok(ApiResponse.ok(parentService.myChildren()));
    }
}
