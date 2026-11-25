package com.tgs.school.notifications.service;

import com.tgs.school.auth.entity.User;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.notifications.entity.Notification;
import com.tgs.school.notifications.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final SmsProvider smsProvider;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationDto {
        private Long id;
        private String title;
        private String message;
        private String notificationType;
        private String referenceType;
        private Long referenceId;
        private boolean isRead;
        private String channel;
        private String deliveryStatus;
        private Instant createdAt;
    }

    @Transactional
    public void notifyUser(User user, String title, String message, String type, String refType, Long refId, boolean sendExternal) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .referenceType(refType)
                .referenceId(refId)
                .channel("IN_APP")
                .deliveryStatus("DELIVERED")
                .build());

        if (sendExternal) {
            emailNotificationService.sendEmail(user.getEmail(), "[TGS] " + title, message);
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                smsProvider.sendSms(user.getPhone(), title + ": " + message);
            }
        }
    }

    @Transactional
    public void notifyUsers(List<User> users, String title, String message, String type, String refType, Long refId) {
        List<Notification> batch = users.stream()
                .map(u -> Notification.builder()
                        .user(u)
                        .title(title)
                        .message(message)
                        .notificationType(type)
                        .referenceType(refType)
                        .referenceId(refId)
                        .channel("IN_APP")
                        .deliveryStatus("DELIVERED")
                        .build())
                .toList();
        notificationRepository.saveAll(batch);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> myNotifications(Long userId, int page, int size) {
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                        .map(this::toDto));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's notification");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .channel(n.getChannel())
                .deliveryStatus(n.getDeliveryStatus())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
