package com.tgs.school.teachers.service;

import com.tgs.school.academics.entity.*;
import com.tgs.school.academics.repository.*;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.teachers.entity.Staff;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.entity.TeacherAssignment;
import com.tgs.school.teachers.repository.StaffRepository;
import com.tgs.school.teachers.repository.TeacherAssignmentRepository;
import com.tgs.school.teachers.repository.TeacherRepository;
import com.tgs.school.users.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    @Data
    public static class CreateTeacherRequest {
        @NotBlank private String fullName;
        @NotBlank @Email private String email;
        @NotBlank private String phone;
        private String employeeCode;
        private String username;
        private String password;
        private String gender;
        private LocalDate dateOfBirth;
        private String qualification;
        private String specialization;
        private LocalDate joiningDate;
        private String address;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TeacherDto {
        private Long id;
        private Long userId;
        private String username;
        private String employeeCode;
        private String fullName;
        private String email;
        private String phone;
        private String gender;
        private LocalDate dateOfBirth;
        private String qualification;
        private String specialization;
        private LocalDate joiningDate;
        private String employmentStatus;
        private String address;
        private boolean accountEnabled;
    }

    @Data
    public static class AssignTeacherRequest {
        @NotNull private Long teacherId;
        private Long academicYearId;
        @NotNull private Long sectionId;
        @NotNull private Long subjectId;
        private boolean isClassTeacher;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TeacherAssignmentDto {
        private Long id;
        private Long teacherId;
        private String teacherName;
        private Long academicYearId;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private boolean isClassTeacher;
    }

    @Data
    public static class CreateStaffRequest {
        @NotBlank private String fullName;
        private String employeeCode;
        @NotBlank private String roleTitle;
        private String department;
        @NotBlank private String phone;
        private String email;
        private LocalDate joiningDate;
        private BigDecimal salary;
        private String address;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StaffDto {
        private Long id;
        private String employeeCode;
        private String fullName;
        private String roleTitle;
        private String department;
        private String phone;
        private String email;
        private LocalDate joiningDate;
        private BigDecimal salary;
        private String status;
        private String address;
    }

    @Transactional
    public TeacherDto createTeacher(CreateTeacherRequest req) {
        String empCode = (req.getEmployeeCode() != null && !req.getEmployeeCode().isBlank())
                ? req.getEmployeeCode().trim().toUpperCase()
                : "TGS-TCH-" + (teacherRepository.count() + 101);
        if (teacherRepository.findByEmployeeCode(empCode).isPresent()) {
            throw new ConflictException("Employee code already exists: " + empCode);
        }
        String username = (req.getUsername() != null && !req.getUsername().isBlank())
                ? req.getUsername().trim()
                : empCode.toLowerCase();
        String password = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : "Teacher@123";

        User user = userService.createUserAccount(
                username, req.getEmail(), req.getPhone(), req.getFullName(), password, Set.of(RoleName.TEACHER));

        Teacher saved = teacherRepository.save(Teacher.builder()
                .user(user)
                .employeeCode(empCode)
                .fullName(req.getFullName().trim())
                .email(req.getEmail().trim().toLowerCase())
                .phone(req.getPhone().trim())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .qualification(req.getQualification())
                .specialization(req.getSpecialization())
                .joiningDate(req.getJoiningDate() != null ? req.getJoiningDate() : LocalDate.now())
                .address(req.getAddress())
                .build());

        auditService.log("CREATE_TEACHER", "TEACHER", saved.getId().toString(), saved.getEmployeeCode());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherDto> listTeachers(String q, Pageable pageable) {
        Page<Teacher> page = (q == null || q.isBlank())
                ? teacherRepository.findAllActive(pageable)
                : teacherRepository.search(q.trim(), pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    @Transactional
    public TeacherDto updateTeacher(Long id, CreateTeacherRequest req) {
        Teacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        t.setFullName(req.getFullName().trim());
        t.setPhone(req.getPhone().trim());
        t.setQualification(req.getQualification());
        t.setSpecialization(req.getSpecialization());
        t.setGender(req.getGender());
        t.setDateOfBirth(req.getDateOfBirth());
        t.setAddress(req.getAddress());
        t.getUser().setFullName(req.getFullName().trim());
        t.getUser().setPhone(req.getPhone().trim());
        userRepository.save(t.getUser());
        return toDto(teacherRepository.save(t));
    }

    @Transactional
    public TeacherAssignmentDto assignTeacher(AssignTeacherRequest req) {
        Teacher teacher = teacherRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        AcademicYear ay = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId()).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : academicYearRepository.findByIsCurrentTrue().orElseThrow(() -> new ResourceNotFoundException("Current year not found"));
        Section section = sectionRepository.findById(req.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        TeacherAssignment ta = assignmentRepository
                .findByAcademicYearIdAndSectionIdAndSubjectId(ay.getId(), section.getId(), subject.getId())
                .orElse(TeacherAssignment.builder()
                        .academicYear(ay)
                        .schoolClass(section.getSchoolClass())
                        .section(section)
                        .subject(subject)
                        .build());
        ta.setTeacher(teacher);
        ta.setClassTeacher(req.isClassTeacher());
        if (req.isClassTeacher()) {
            section.setClassTeacher(teacher);
            sectionRepository.save(section);
        }
        TeacherAssignment saved = assignmentRepository.save(ta);
        auditService.log("ASSIGN_TEACHER", "TEACHER_ASSIGNMENT", saved.getId().toString(),
                teacher.getFullName() + " -> " + section.getSchoolClass().getName() + "-" + section.getName() + " (" + subject.getName() + ")");
        return toAssignmentDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentDto> listAssignments(Long yearId, Long teacherId) {
        Long resolvedYear = yearId != null ? yearId
                : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId).orElse(null);
        if (resolvedYear == null) return List.of();
        if (teacherId != null) {
            return assignmentRepository.findByTeacherAndYear(teacherId, resolvedYear).stream()
                    .map(this::toAssignmentDto).toList();
        }
        return assignmentRepository.findAllByYear(resolvedYear).stream()
                .map(this::toAssignmentDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentDto> myAssignments() {
        Teacher teacher = accessControlService.requireCurrentTeacher();
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active academic year"));
        return assignmentRepository.findByTeacherAndYear(teacher.getId(), ay.getId()).stream()
                .map(this::toAssignmentDto).toList();
    }

    @Transactional
    public StaffDto createStaff(CreateStaffRequest req) {
        String empCode = (req.getEmployeeCode() != null && !req.getEmployeeCode().isBlank())
                ? req.getEmployeeCode().trim().toUpperCase()
                : "TGS-STF-" + (staffRepository.count() + 101);
        if (staffRepository.existsByEmployeeCode(empCode)) {
            throw new ConflictException("Employee code already exists: " + empCode);
        }
        Staff saved = staffRepository.save(Staff.builder()
                .employeeCode(empCode)
                .fullName(req.getFullName().trim())
                .roleTitle(req.getRoleTitle().trim())
                .department(req.getDepartment())
                .phone(req.getPhone().trim())
                .email(req.getEmail())
                .joiningDate(req.getJoiningDate() != null ? req.getJoiningDate() : LocalDate.now())
                .salary(req.getSalary())
                .address(req.getAddress())
                .build());
        return toStaffDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffDto> listStaff(Pageable pageable) {
        return PageResponse.from(staffRepository.findByDeletedAtIsNull(pageable).map(this::toStaffDto));
    }

    private TeacherDto toDto(Teacher t) {
        return TeacherDto.builder()
                .id(t.getId()).userId(t.getUser().getId()).username(t.getUser().getUsername())
                .employeeCode(t.getEmployeeCode()).fullName(t.getFullName()).email(t.getEmail())
                .phone(t.getPhone()).gender(t.getGender()).dateOfBirth(t.getDateOfBirth())
                .qualification(t.getQualification()).specialization(t.getSpecialization())
                .joiningDate(t.getJoiningDate()).employmentStatus(t.getEmploymentStatus())
                .address(t.getAddress()).accountEnabled(t.getUser().isEnabled())
                .build();
    }

    private TeacherAssignmentDto toAssignmentDto(TeacherAssignment ta) {
        return TeacherAssignmentDto.builder()
                .id(ta.getId())
                .teacherId(ta.getTeacher().getId())
                .teacherName(ta.getTeacher().getFullName())
                .academicYearId(ta.getAcademicYear().getId())
                .classId(ta.getSchoolClass().getId())
                .className(ta.getSchoolClass().getName())
                .sectionId(ta.getSection().getId())
                .sectionName(ta.getSection().getName())
                .subjectId(ta.getSubject().getId())
                .subjectName(ta.getSubject().getName())
                .subjectCode(ta.getSubject().getCode())
                .isClassTeacher(ta.isClassTeacher())
                .build();
    }

    private StaffDto toStaffDto(Staff s) {
        return StaffDto.builder()
                .id(s.getId()).employeeCode(s.getEmployeeCode()).fullName(s.getFullName())
                .roleTitle(s.getRoleTitle()).department(s.getDepartment()).phone(s.getPhone())
                .email(s.getEmail()).joiningDate(s.getJoiningDate()).salary(s.getSalary())
                .status(s.getStatus()).address(s.getAddress()).build();
    }
}
