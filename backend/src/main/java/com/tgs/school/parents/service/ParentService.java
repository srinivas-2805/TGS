package com.tgs.school.parents.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.parents.entity.ParentProfile;
import com.tgs.school.parents.repository.ParentRepository;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.entity.StudentParent;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentParentRepository;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.users.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserService userService;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    @Data
    public static class CreateParentRequest {
        @NotBlank private String fullName;
        @NotBlank @Email private String email;
        @NotBlank private String phone;
        private String alternatePhone;
        private String occupation;
        private String address;
        private String city = "Kalluru";
        private String state = "Telangana";
        private String pincode;
        private String username;
        private String password;
    }

    @Data
    public static class LinkStudentRequest {
        @NotNull private Long parentId;
        @NotNull private Long studentId;
        private String relationship = "GUARDIAN";
        private boolean isPrimary = true;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParentDto {
        private Long id;
        private Long userId;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private String alternatePhone;
        private String occupation;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private List<ChildSummaryDto> children;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChildSummaryDto {
        private Long studentId;
        private String admissionNumber;
        private String fullName;
        private String relationship;
        private String className;
        private String sectionName;
        private String rollNumber;
        private String status;
        private Long classId;
        private Long sectionId;
    }

    @Transactional
    public ParentDto createParent(CreateParentRequest req) {
        ParentProfile saved = findOrCreateParent(req);
        auditService.log("CREATE_PARENT", "PARENT", saved.getId().toString(), saved.getFullName());
        return toDto(saved);
    }

    @Transactional
    public ParentProfile findOrCreateParent(CreateParentRequest req) {
        var existingByEmail = parentRepository.findByEmailAndDeletedAtIsNull(req.getEmail().trim().toLowerCase());
        if (existingByEmail.isPresent()) {
            return existingByEmail.get();
        }
        String username = (req.getUsername() != null && !req.getUsername().isBlank())
                ? req.getUsername().trim()
                : "parent." + req.getPhone().replaceAll("[^0-9]", "");
        String password = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : "Parent@123";
        User user = userService.createUserAccount(
                username, req.getEmail(), req.getPhone(), req.getFullName(), password, Set.of(RoleName.PARENT));
        return parentRepository.save(ParentProfile.builder()
                .user(user)
                .fullName(req.getFullName().trim())
                .email(req.getEmail().trim().toLowerCase())
                .phone(req.getPhone().trim())
                .alternatePhone(req.getAlternatePhone())
                .occupation(req.getOccupation())
                .address(req.getAddress())
                .city(req.getCity() != null ? req.getCity() : "Kalluru")
                .state(req.getState() != null ? req.getState() : "Telangana")
                .pincode(req.getPincode())
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<ParentDto> listParents(String q, Pageable pageable) {
        Page<ParentProfile> page = (q == null || q.isBlank())
                ? parentRepository.findAllActive(pageable)
                : parentRepository.search(q.trim(), pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    @Transactional
    public ParentDto linkStudent(LinkStudentRequest req) {
        ParentProfile parent = parentRepository.findById(req.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        Student student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean alreadyLinked = studentParentRepository.findByStudentIdWithParent(student.getId()).stream()
                .anyMatch(sp -> sp.getParent().getId().equals(parent.getId()));
        if (alreadyLinked) {
            throw new ConflictException("Student is already linked to this parent");
        }
        studentParentRepository.save(StudentParent.builder()
                .student(student)
                .parent(parent)
                .relationship(req.getRelationship() != null ? req.getRelationship() : "GUARDIAN")
                .isPrimary(req.isPrimary())
                .build());
        auditService.log("LINK_PARENT_STUDENT", "STUDENT_PARENT", student.getId() + "-" + parent.getId(),
                "Linked " + student.getFullName() + " to " + parent.getFullName());
        return toDto(parent);
    }

    @Transactional(readOnly = true)
    public List<ChildSummaryDto> myChildren() {
        ParentProfile parent = accessControlService.requireCurrentParent();
        return getChildrenForParent(parent.getId());
    }

    private List<ChildSummaryDto> getChildrenForParent(Long parentId) {
        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);
        return studentParentRepository.findByParentIdWithStudent(parentId).stream().map(sp -> {
            Student s = sp.getStudent();
            StudentEnrollment en = currentYear != null
                    ? enrollmentRepository.findByStudentAndYear(s.getId(), currentYear.getId()).orElse(null)
                    : null;
            return ChildSummaryDto.builder()
                    .studentId(s.getId())
                    .admissionNumber(s.getAdmissionNumber())
                    .fullName(s.getFullName())
                    .relationship(sp.getRelationship())
                    .className(en != null ? en.getSchoolClass().getName() : null)
                    .sectionName(en != null ? en.getSection().getName() : null)
                    .rollNumber(en != null ? en.getRollNumber() : null)
                    .classId(en != null ? en.getSchoolClass().getId() : null)
                    .sectionId(en != null ? en.getSection().getId() : null)
                    .status(s.getStatus())
                    .build();
        }).toList();
    }

    private ParentDto toDto(ParentProfile p) {
        return ParentDto.builder()
                .id(p.getId()).userId(p.getUser().getId()).username(p.getUser().getUsername())
                .fullName(p.getFullName()).email(p.getEmail()).phone(p.getPhone())
                .alternatePhone(p.getAlternatePhone()).occupation(p.getOccupation())
                .address(p.getAddress()).city(p.getCity()).state(p.getState()).pincode(p.getPincode())
                .children(getChildrenForParent(p.getId()))
                .build();
    }
}
