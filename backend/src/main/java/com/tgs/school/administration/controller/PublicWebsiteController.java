package com.tgs.school.administration.controller;

import com.tgs.school.academics.service.AcademicService;
import com.tgs.school.administration.entity.AdmissionEnquiry;
import com.tgs.school.administration.service.WebsiteCmsService;
import com.tgs.school.administration.service.WebsiteCmsService.SubmitEnquiryRequest;
import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.communication.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicWebsiteController {

    private final WebsiteCmsService cmsService;
    private final AnnouncementService announcementService;
    private final AcademicService academicService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "content", cmsService.getPublishedContent(),
                "announcements", announcementService.listPublic(),
                "events", cmsService.getPublicEvents(),
                "gallery", cmsService.getPublishedGallery(),
                "classes", academicService.listClasses()
        )));
    }

    @PostMapping("/enquiries")
    public ResponseEntity<ApiResponse<AdmissionEnquiry>> submitEnquiry(@Valid @RequestBody SubmitEnquiryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Admission enquiry submitted. Our school office will contact you shortly.",
                        cmsService.submitEnquiry(req)));
    }
}
