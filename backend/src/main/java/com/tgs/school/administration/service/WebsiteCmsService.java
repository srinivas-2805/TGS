package com.tgs.school.administration.service;

import com.tgs.school.administration.entity.AdmissionEnquiry;
import com.tgs.school.administration.entity.GalleryItem;
import com.tgs.school.administration.entity.SchoolEvent;
import com.tgs.school.administration.entity.WebsiteContent;
import com.tgs.school.administration.repository.AdmissionEnquiryRepository;
import com.tgs.school.administration.repository.GalleryRepository;
import com.tgs.school.administration.repository.SchoolEventRepository;
import com.tgs.school.administration.repository.WebsiteContentRepository;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebsiteCmsService {

    private final WebsiteContentRepository contentRepository;
    private final GalleryRepository galleryRepository;
    private final SchoolEventRepository eventRepository;
    private final AdmissionEnquiryRepository enquiryRepository;
    private final UserRepository userRepository;

    @Data
    public static class UpsertContentRequest {
        @NotBlank private String sectionKey;
        @NotBlank private String title;
        private String subtitle;
        @NotBlank private String bodyContent;
        private String metadataJson;
        private String imageUrl;
        private boolean isPublished = true;
    }

    @Data
    public static class CreateGalleryRequest {
        @NotBlank private String title;
        private String description;
        @NotBlank private String category;
        @NotBlank private String imageUrl;
        private Integer displayOrder = 0;
        private boolean isPublished = true;
    }

    @Data
    public static class CreateEventRequest {
        @NotBlank private String title;
        @NotBlank private String description;
        @NotNull private LocalDate eventDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String venue;
        private String category = "ACADEMIC";
        private String imageUrl;
        private boolean isPublic = true;
    }

    @Data
    public static class SubmitEnquiryRequest {
        @NotBlank private String studentName;
        @NotBlank private String parentName;
        @NotBlank private String phone;
        private String email;
        @NotBlank private String applyingClassName;
        private String previousSchool;
        private String message;
    }

    @Transactional(readOnly = true)
    public List<WebsiteContent> getPublishedContent() {
        return contentRepository.findByIsPublishedTrueOrderBySectionKeyAsc();
    }

    @Transactional(readOnly = true)
    public List<WebsiteContent> getAllContent() {
        return contentRepository.findAllByOrderBySectionKeyAsc();
    }

    @Transactional
    public WebsiteContent upsertContent(UpsertContentRequest req) {
        User editor = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        WebsiteContent wc = contentRepository.findBySectionKey(req.getSectionKey().trim().toUpperCase())
                .orElse(WebsiteContent.builder().sectionKey(req.getSectionKey().trim().toUpperCase()).build());
        wc.setTitle(req.getTitle().trim());
        wc.setSubtitle(req.getSubtitle());
        wc.setBodyContent(req.getBodyContent());
        wc.setMetadataJson(req.getMetadataJson());
        wc.setImageUrl(req.getImageUrl());
        wc.setPublished(req.isPublished());
        wc.setUpdatedBy(editor);
        return contentRepository.save(wc);
    }

    @Transactional(readOnly = true)
    public List<GalleryItem> getPublishedGallery() {
        return galleryRepository.findByIsPublishedTrueOrderByDisplayOrderAscCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<GalleryItem> getAllGallery() {
        return galleryRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc();
    }

    @Transactional
    public GalleryItem addGalleryItem(CreateGalleryRequest req) {
        User creator = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        return galleryRepository.save(GalleryItem.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .category(req.getCategory().toUpperCase())
                .imageUrl(req.getImageUrl().trim())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .isPublished(req.isPublished())
                .createdBy(creator)
                .build());
    }

    @Transactional
    public void deleteGalleryItem(Long id) {
        galleryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SchoolEvent> getPublicEvents() {
        return eventRepository.findByIsPublicTrueOrderByEventDateDesc();
    }

    @Transactional(readOnly = true)
    public List<SchoolEvent> getAllEvents() {
        return eventRepository.findAllByOrderByEventDateDesc();
    }

    @Transactional
    public SchoolEvent createEvent(CreateEventRequest req) {
        User creator = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        return eventRepository.save(SchoolEvent.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription().trim())
                .eventDate(req.getEventDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .venue(req.getVenue())
                .category(req.getCategory() != null ? req.getCategory().toUpperCase() : "ACADEMIC")
                .imageUrl(req.getImageUrl())
                .isPublic(req.isPublic())
                .createdBy(creator)
                .build());
    }

    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    @Transactional
    public AdmissionEnquiry submitEnquiry(SubmitEnquiryRequest req) {
        return enquiryRepository.save(AdmissionEnquiry.builder()
                .studentName(req.getStudentName().trim())
                .parentName(req.getParentName().trim())
                .phone(req.getPhone().trim())
                .email(req.getEmail())
                .applyingClassName(req.getApplyingClassName().trim())
                .previousSchool(req.getPreviousSchool())
                .message(req.getMessage())
                .status("NEW")
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdmissionEnquiry> listEnquiries(Pageable pageable) {
        return PageResponse.from(enquiryRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @Transactional
    public AdmissionEnquiry updateEnquiryStatus(Long id, String status, String notes) {
        AdmissionEnquiry eq = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
        eq.setStatus(status.toUpperCase());
        if (notes != null) eq.setNotes(notes);
        return enquiryRepository.save(eq);
    }
}
