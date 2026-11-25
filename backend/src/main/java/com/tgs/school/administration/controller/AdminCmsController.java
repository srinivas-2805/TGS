package com.tgs.school.administration.controller;

import com.tgs.school.administration.entity.AdmissionEnquiry;
import com.tgs.school.administration.entity.GalleryItem;
import com.tgs.school.administration.entity.SchoolEvent;
import com.tgs.school.administration.entity.WebsiteContent;
import com.tgs.school.administration.service.WebsiteCmsService;
import com.tgs.school.administration.service.WebsiteCmsService.*;
import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
public class AdminCmsController {

    private final WebsiteCmsService cmsService;
    private final com.tgs.school.common.audit.AuditLogRepository auditLogRepository;

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<com.tgs.school.common.audit.AuditLog>>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)))));
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<List<WebsiteContent>>> listContent() {
        return ResponseEntity.ok(ApiResponse.ok(cmsService.getAllContent()));
    }

    @PutMapping("/content")
    public ResponseEntity<ApiResponse<WebsiteContent>> upsertContent(@Valid @RequestBody UpsertContentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Website content saved", cmsService.upsertContent(req)));
    }

    @GetMapping("/gallery")
    public ResponseEntity<ApiResponse<List<GalleryItem>>> listGallery() {
        return ResponseEntity.ok(ApiResponse.ok(cmsService.getAllGallery()));
    }

    @PostMapping("/gallery")
    public ResponseEntity<ApiResponse<GalleryItem>> addGallery(@Valid @RequestBody CreateGalleryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Gallery item added", cmsService.addGalleryItem(req)));
    }

    @DeleteMapping("/gallery/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGallery(@PathVariable Long id) {
        cmsService.deleteGalleryItem(id);
        return ResponseEntity.ok(ApiResponse.okMessage("Gallery item removed"));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<SchoolEvent>>> listEvents() {
        return ResponseEntity.ok(ApiResponse.ok(cmsService.getAllEvents()));
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<SchoolEvent>> createEvent(@Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Event created", cmsService.createEvent(req)));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        cmsService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.okMessage("Event removed"));
    }

    @GetMapping("/enquiries")
    public ResponseEntity<ApiResponse<PageResponse<AdmissionEnquiry>>> listEnquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(cmsService.listEnquiries(PageRequest.of(page, size))));
    }

    @PatchMapping("/enquiries/{id}")
    public ResponseEntity<ApiResponse<AdmissionEnquiry>> updateEnquiry(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Enquiry updated",
                cmsService.updateEnquiryStatus(id, body.getOrDefault("status", "CONTACTED"), body.get("notes"))));
    }
}
