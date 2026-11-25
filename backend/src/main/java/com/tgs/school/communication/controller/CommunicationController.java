package com.tgs.school.communication.controller;

import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.dto.ApiResponse;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.communication.service.AnnouncementService;
import com.tgs.school.communication.service.AnnouncementService.*;
import com.tgs.school.notifications.service.NotificationService;
import com.tgs.school.notifications.service.NotificationService.NotificationDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunicationController {

    private final AnnouncementService announcementService;
    private final NotificationService notificationService;

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> listAnnouncements() {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.listAll()));
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN','TEACHER')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(@Valid @RequestBody CreateAnnouncementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Announcement published", announcementService.create(req)));
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.okMessage("Announcement deleted"));
    }

    @GetMapping("/notifications/my")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> myNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.myNotifications(SecurityUtils.getCurrentUserId(), page, size)));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.unreadCount(SecurityUtils.getCurrentUserId())));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.okMessage("Notification marked as read"));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.okMessage("All notifications marked as read"));
    }
}
