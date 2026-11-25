package com.tgs.school.communication.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.SchoolClass;
import com.tgs.school.academics.entity.Section;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SchoolClassRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.communication.entity.Announcement;
import com.tgs.school.communication.repository.AnnouncementRepository;
import com.tgs.school.notifications.service.NotificationService;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.entity.StudentParent;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentParentRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Data
    public static class CreateAnnouncementRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        private String category = "GENERAL";
        private String targetAudience = "ALL";
        private Long targetClassId;
        private Long targetSectionId;
        private boolean isPublic = false;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AnnouncementDto {
        private Long id;
        private String title;
        private String content;
        private String category;
        private String targetAudience;
        private Long targetClassId;
        private String targetClassName;
        private Long targetSectionId;
        private String targetSectionName;
        private boolean isPublic;
        private String publishedByName;
        private Instant publishedAt;
    }

    @Transactional
    public AnnouncementDto create(CreateAnnouncementRequest req) {
        User publisher = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
        SchoolClass targetClass = req.getTargetClassId() != null
                ? classRepository.findById(req.getTargetClassId()).orElseThrow(() -> new ResourceNotFoundException("Class not found"))
                : null;
        Section targetSection = req.getTargetSectionId() != null
                ? sectionRepository.findById(req.getTargetSectionId()).orElseThrow(() -> new ResourceNotFoundException("Section not found"))
                : null;

        Announcement saved = announcementRepository.save(Announcement.builder()
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .category(req.getCategory() != null ? req.getCategory() : "GENERAL")
                .targetAudience(req.getTargetAudience() != null ? req.getTargetAudience() : "ALL")
                .targetClass(targetClass)
                .targetSection(targetSection)
                .isPublic(req.isPublic())
                .publishedBy(publisher)
                .publishedAt(Instant.now())
                .build());

        List<User> recipients = resolveRecipients(saved);
        if (!recipients.isEmpty()) {
            notificationService.notifyUsers(recipients, saved.getTitle(), saved.getContent(),
                    "ANNOUNCEMENT", "ANNOUNCEMENT", saved.getId());
        }
        auditService.log("CREATE_ANNOUNCEMENT", "ANNOUNCEMENT", saved.getId().toString(),
                "Audience=" + saved.getTargetAudience() + ", recipients=" + recipients.size());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> listAll() {
        return announcementRepository.findAllWithDetails().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> listPublic() {
        return announcementRepository.findTop10ByIsPublicTrueOrderByPublishedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }

    private List<User> resolveRecipients(Announcement a) {
        Map<Long, User> uniqueUsers = new LinkedHashMap<>();
        String audience = a.getTargetAudience();
        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);

        if ("ALL".equalsIgnoreCase(audience)) {
            userRepository.findAll().stream()
                    .filter(u -> u.isEnabled() && u.getDeletedAt() == null)
                    .forEach(u -> uniqueUsers.put(u.getId(), u));
        } else if ("TEACHERS".equalsIgnoreCase(audience)) {
            userRepository.findActiveByRole(RoleName.TEACHER).forEach(u -> uniqueUsers.put(u.getId(), u));
        } else if ("PARENTS".equalsIgnoreCase(audience)) {
            userRepository.findActiveByRole(RoleName.PARENT).forEach(u -> uniqueUsers.put(u.getId(), u));
        } else if ("STUDENTS".equalsIgnoreCase(audience)) {
            userRepository.findActiveByRole(RoleName.STUDENT).forEach(u -> uniqueUsers.put(u.getId(), u));
        } else if ("SECTION".equalsIgnoreCase(audience) && a.getTargetSection() != null && currentYear != null) {
            List<StudentEnrollment> enrollments = enrollmentRepository.findActiveByYearAndSection(currentYear.getId(), a.getTargetSection().getId());
            addStudentAndParentUsers(enrollments, uniqueUsers);
        } else if ("CLASS".equalsIgnoreCase(audience) && a.getTargetClass() != null && currentYear != null) {
            List<StudentEnrollment> enrollments = enrollmentRepository.findActiveByYearAndClass(currentYear.getId(), a.getTargetClass().getId());
            addStudentAndParentUsers(enrollments, uniqueUsers);
        }
        return new ArrayList<>(uniqueUsers.values());
    }

    private void addStudentAndParentUsers(List<StudentEnrollment> enrollments, Map<Long, User> uniqueUsers) {
        for (StudentEnrollment e : enrollments) {
            User stuUser = e.getStudent().getUser();
            if (stuUser.isEnabled()) uniqueUsers.put(stuUser.getId(), stuUser);
            for (StudentParent sp : studentParentRepository.findByStudentIdWithParent(e.getStudent().getId())) {
                User pUser = sp.getParent().getUser();
                if (pUser.isEnabled()) uniqueUsers.put(pUser.getId(), pUser);
            }
        }
    }

    private AnnouncementDto toDto(Announcement a) {
        return AnnouncementDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .category(a.getCategory())
                .targetAudience(a.getTargetAudience())
                .targetClassId(a.getTargetClass() != null ? a.getTargetClass().getId() : null)
                .targetClassName(a.getTargetClass() != null ? a.getTargetClass().getName() : null)
                .targetSectionId(a.getTargetSection() != null ? a.getTargetSection().getId() : null)
                .targetSectionName(a.getTargetSection() != null ? a.getTargetSection().getName() : null)
                .isPublic(a.isPublic())
                .publishedByName(a.getPublishedBy() != null ? a.getPublishedBy().getFullName() : "School Administration")
                .publishedAt(a.getPublishedAt())
                .build();
    }
}
