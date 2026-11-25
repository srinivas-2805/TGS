package com.tgs.school.library.controller;

import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.library.entity.Book;
import com.tgs.school.library.service.LibraryService;
import com.tgs.school.library.service.LibraryService.*;
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
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping("/books")
    public ResponseEntity<ApiResponse<PageResponse<Book>>> searchBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                libraryService.searchBooks(q, category, PageRequest.of(page, size, Sort.by("title")))));
    }

    @PostMapping("/books")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<Book>> addBook(@Valid @RequestBody CreateBookRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Book added", libraryService.addBook(req)));
    }

    @GetMapping("/issues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','LIBRARIAN','STUDENT','PARENT')")
    public ResponseEntity<ApiResponse<List<BookIssueDto>>> listIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(libraryService.listIssues(status, studentId)));
    }

    @PostMapping("/issues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookIssueDto>> issueBook(@Valid @RequestBody IssueBookRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Book issued", libraryService.issueBook(req)));
    }

    @PatchMapping("/issues/{id}/return")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookIssueDto>> returnBook(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean lateFeePaid) {
        return ResponseEntity.ok(ApiResponse.ok("Book returned", libraryService.returnBook(id, lateFeePaid)));
    }
}
