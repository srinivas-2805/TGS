package com.tgs.school.attendance.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.attendance.entity.Attendance;
import com.tgs.school.attendance.entity.LeaveRequest;
import com.tgs.school.attendance.entity.TeacherAttendance;
import com.tgs.school.attendance.repository.AttendanceRepository;
import com.tgs.school.attendance.repository.LeaveRequestRepository;
import com.tgs.school.attendance.repository.TeacherAttendanceRepository;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.notifications.service.NotificationService;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.teachers.entity.Teacher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Data
    public static class ApplyLeaveRequest {
        private Long studentId;
        @NotNull private LocalDate startDate;
        @NotNull private LocalDate endDate;
        @NotBlank private String reason;
    }

    @Data
    public static class ReviewLeaveRequest {
        @NotBlank private String decision; // APPROVED or REJECTED
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LeaveRequestDto {
        private Long id;
        private String applicantType;
        private Long studentId;
        private String studentName;
        private Long teacherId;
        private String teacherName;
        private String requestedByName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private String status;
        private String reviewRemarks;
        private Instant createdAt;
        private Instant reviewedAt;
    }

    @Transactional
    public LeaveRequestDto applyStudentLeave(ApplyLeaveRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
        Student student;
        if (SecurityUtils.hasAnyRole(RoleName.STUDENT)) {
            student = accessControlService.requireCurrentStudent();
        } else {
            if (req.getStudentId() == null) {
                throw new BadRequestException("studentId is required");
            }
            accessControlService.assertCanAccessStudent(req.getStudentId());
            student = studentRepository.findById(req.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        }
        User requester = userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow();
        LeaveRequest saved = leaveRepository.save(LeaveRequest.builder()
                .applicantType("STUDENT")
                .student(student)
                .requestedBy(requester)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason().trim())
                .status("PENDING")
                .build());
        auditService.log("APPLY_LEAVE", "LEAVE_REQUEST", saved.getId().toString(),
                "Student leave: " + student.getFullName() + " (" + req.getStartDate() + " to " + req.getEndDate() + ")");
        return toDto(saved);
    }

    @Transactional
    public LeaveRequestDto applyTeacherLeave(ApplyLeaveRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
        Teacher teacher = accessControlService.requireCurrentTeacher();
        User requester = teacher.getUser();
        LeaveRequest saved = leaveRepository.save(LeaveRequest.builder()
                .applicantType("TEACHER")
                .teacher(teacher)
                .requestedBy(requester)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason().trim())
                .status("PENDING")
                .build());
        auditService.log("APPLY_TEACHER_LEAVE", "LEAVE_REQUEST", saved.getId().toString(),
                "Teacher leave: " + teacher.getFullName());
        return toDto(saved);
    }

    @Transactional
    public LeaveRequestDto reviewLeave(Long id, ReviewLeaveRequest req) {
        LeaveRequest lr = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        String decision = req.getDecision().toUpperCase();
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new BadRequestException("Decision must be APPROVED or REJECTED");
        }
        User reviewer = userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow();

        if ("TEACHER".equals(lr.getApplicantType()) && !SecurityUtils.hasAnyRole(RoleName.SUPER_ADMIN, RoleName.PRINCIPAL, RoleName.ADMIN)) {
            throw new BadRequestException("Only Principal or Admin can review teacher leave requests");
        }

        lr.setStatus(decision);
        lr.setReviewedBy(reviewer);
        lr.setReviewRemarks(req.getRemarks());
        lr.setReviewedAt(Instant.now());
        LeaveRequest saved = leaveRepository.save(lr);

        if ("APPROVED".equals(decision)) {
            applyApprovedLeaveToAttendance(saved, reviewer);
        }

        String applicantName = saved.getStudent() != null ? saved.getStudent().getFullName() : saved.getTeacher().getFullName();
        notificationService.notifyUser(
                saved.getRequestedBy(),
                "Leave Request " + decision,
                "Leave request for " + applicantName + " (" + saved.getStartDate() + " to " + saved.getEndDate() + ") has been " + decision + ".",
                "LEAVE_STATUS",
                "LEAVE_REQUEST",
                saved.getId(),
                true
        );

        auditService.log("REVIEW_LEAVE", "LEAVE_REQUEST", id.toString(), "Decision=" + decision);
        return toDto(saved);
    }

    private void applyApprovedLeaveToAttendance(LeaveRequest lr, User reviewer) {
        LocalDate date = lr.getStartDate();
        if ("STUDENT".equals(lr.getApplicantType()) && lr.getStudent() != null) {
            AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);
            if (currentYear == null) return;
            StudentEnrollment en = enrollmentRepository.findByStudentAndYear(lr.getStudent().getId(), currentYear.getId()).orElse(null);
            if (en == null) return;
            while (!date.isAfter(lr.getEndDate())) {
                LocalDate d = date;
                Attendance att = attendanceRepository.findByStudentIdAndAttendanceDateAndSession(lr.getStudent().getId(), d, "FULL_DAY")
                        .orElse(Attendance.builder()
                                .student(lr.getStudent())
                                .enrollment(en)
                                .academicYear(currentYear)
                                .schoolClass(en.getSchoolClass())
                                .section(en.getSection())
                                .attendanceDate(d)
                                .session("FULL_DAY")
                                .markedBy(reviewer)
                                .build());
                att.setStatus("APPROVED_LEAVE");
                att.setRemarks("Approved leave #" + lr.getId());
                attendanceRepository.save(att);
                date = date.plusDays(1);
            }
        } else if ("TEACHER".equals(lr.getApplicantType()) && lr.getTeacher() != null) {
            while (!date.isAfter(lr.getEndDate())) {
                LocalDate d = date;
                TeacherAttendance ta = teacherAttendanceRepository.findByTeacherIdAndAttendanceDate(lr.getTeacher().getId(), d)
                        .orElse(TeacherAttendance.builder()
                                .teacher(lr.getTeacher())
                                .attendanceDate(d)
                                .markedBy(reviewer)
                                .build());
                ta.setStatus("APPROVED_LEAVE");
                ta.setRemarks("Approved leave #" + lr.getId());
                teacherAttendanceRepository.save(ta);
                date = date.plusDays(1);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listAll() {
        return leaveRepository.findAllWithDetails().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listForStudent(Long studentId) {
        accessControlService.assertCanAccessStudent(studentId);
        return leaveRepository.findByStudentId(studentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> myTeacherLeaves() {
        Teacher t = accessControlService.requireCurrentTeacher();
        return leaveRepository.findByTeacherId(t.getId()).stream().map(this::toDto).toList();
    }

    private LeaveRequestDto toDto(LeaveRequest lr) {
        return LeaveRequestDto.builder()
                .id(lr.getId())
                .applicantType(lr.getApplicantType())
                .studentId(lr.getStudent() != null ? lr.getStudent().getId() : null)
                .studentName(lr.getStudent() != null ? lr.getStudent().getFullName() : null)
                .teacherId(lr.getTeacher() != null ? lr.getTeacher().getId() : null)
                .teacherName(lr.getTeacher() != null ? lr.getTeacher().getFullName() : null)
                .requestedByName(lr.getRequestedBy().getFullName())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .reviewRemarks(lr.getReviewRemarks())
                .createdAt(lr.getCreatedAt())
                .reviewedAt(lr.getReviewedAt())
                .build();
    }
}
