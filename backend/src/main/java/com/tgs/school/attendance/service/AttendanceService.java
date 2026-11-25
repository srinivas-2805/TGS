package com.tgs.school.attendance.service;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.entity.Section;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.attendance.entity.Attendance;
import com.tgs.school.attendance.entity.TeacherAttendance;
import com.tgs.school.attendance.repository.AttendanceRepository;
import com.tgs.school.attendance.repository.TeacherAttendanceRepository;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.common.security.AccessControlService;
import com.tgs.school.notifications.service.NotificationService;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.entity.StudentParent;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentParentRepository;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.repository.TeacherRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentParentRepository studentParentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Data
    public static class MarkStudentAttendanceRequest {
        private Long academicYearId;
        @NotNull private Long sectionId;
        @NotNull private LocalDate attendanceDate;
        private String session = "FULL_DAY";
        @NotEmpty @Valid private List<StudentAttendanceEntry> records;
    }

    @Data
    public static class StudentAttendanceEntry {
        @NotNull private Long studentId;
        @NotBlank private String status; // PRESENT, ABSENT, LATE, HALF_DAY, APPROVED_LEAVE
        private String remarks;
    }

    @Data
    public static class CorrectAttendanceRequest {
        @NotBlank private String status;
        @NotBlank private String reason;
    }

    @Data
    public static class MarkTeacherAttendanceRequest {
        @NotNull private LocalDate attendanceDate;
        @NotEmpty @Valid private List<TeacherAttendanceEntry> records;
    }

    @Data
    public static class TeacherAttendanceEntry {
        @NotNull private Long teacherId;
        @NotBlank private String status;
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceRecordDto {
        private Long id;
        private Long studentId;
        private String admissionNumber;
        private String studentName;
        private String rollNumber;
        private String className;
        private String sectionName;
        private LocalDate attendanceDate;
        private String session;
        private String status;
        private String remarks;
        private String correctionReason;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentAttendanceSummaryDto {
        private Long studentId;
        private long totalDays;
        private long presentDays;
        private long absentDays;
        private long lateDays;
        private long halfDays;
        private long leaveDays;
        private double attendancePercentage;
        private List<AttendanceRecordDto> records;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TeacherAttendanceDto {
        private Long id;
        private Long teacherId;
        private String employeeCode;
        private String teacherName;
        private LocalDate attendanceDate;
        private String status;
        private String remarks;
    }

    @Transactional
    public List<AttendanceRecordDto> markStudentAttendance(MarkStudentAttendanceRequest req) {
        AcademicYear ay = req.getAcademicYearId() != null
                ? academicYearRepository.findById(req.getAcademicYearId()).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"))
                : academicYearRepository.findByIsCurrentTrue().orElseThrow(() -> new ResourceNotFoundException("Current academic year not found"));
        Section section = sectionRepository.findById(req.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        String session = req.getSession() != null ? req.getSession().toUpperCase() : "FULL_DAY";

        if (!accessControlService.isSchoolWideStaff()) {
            Teacher teacher = accessControlService.requireCurrentTeacher();
            accessControlService.assertTeacherAssignedToSection(teacher.getId(), section.getId(), ay.getId());
        }

        User marker = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        List<AttendanceRecordDto> results = new ArrayList<>();

        for (StudentAttendanceEntry entry : req.getRecords()) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + entry.getStudentId()));
            StudentEnrollment en = enrollmentRepository.findByStudentAndYear(student.getId(), ay.getId())
                    .orElseThrow(() -> new BadRequestException("Student " + student.getFullName() + " is not enrolled in this academic year"));
            if (!en.getSection().getId().equals(section.getId())) {
                throw new BadRequestException("Student " + student.getFullName() + " does not belong to selected section");
            }
            if (attendanceRepository.findByStudentIdAndAttendanceDateAndSession(student.getId(), req.getAttendanceDate(), session).isPresent()) {
                throw new ConflictException("Attendance already marked for " + student.getFullName() + " on " + req.getAttendanceDate() + " (" + session + ")");
            }

            String status = entry.getStatus().toUpperCase();
            Attendance saved = attendanceRepository.save(Attendance.builder()
                    .student(student)
                    .enrollment(en)
                    .academicYear(ay)
                    .schoolClass(section.getSchoolClass())
                    .section(section)
                    .attendanceDate(req.getAttendanceDate())
                    .session(session)
                    .status(status)
                    .remarks(entry.getRemarks())
                    .markedBy(marker)
                    .build());

            if ("ABSENT".equals(status)) {
                notifyParentsOfAbsence(student, req.getAttendanceDate(), section);
            }
            results.add(toDto(saved, en.getRollNumber()));
        }

        auditService.log("MARK_ATTENDANCE", "ATTENDANCE", section.getId() + ":" + req.getAttendanceDate(),
                "Marked " + results.size() + " records for " + section.getSchoolClass().getName() + "-" + section.getName());
        return results;
    }

    @Transactional
    public AttendanceRecordDto correctAttendance(Long attendanceId, CorrectAttendanceRequest req) {
        if (!SecurityUtils.hasAnyRole(RoleName.SUPER_ADMIN, RoleName.PRINCIPAL, RoleName.ADMIN)) {
            throw new AccessDeniedException("Only Principal or Admin can correct saved attendance records.");
        }
        Attendance record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        User corrector = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        record.setStatus(req.getStatus().toUpperCase());
        record.setCorrectedBy(corrector);
        record.setCorrectionReason(req.getReason().trim());
        Attendance saved = attendanceRepository.save(record);
        auditService.log("CORRECT_ATTENDANCE", "ATTENDANCE", attendanceId.toString(),
                "New status=" + saved.getStatus() + ", reason=" + req.getReason());
        return toDto(saved, saved.getEnrollment().getRollNumber());
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getSectionAttendance(Long sectionId, LocalDate date, String session) {
        String resolvedSession = session != null ? session.toUpperCase() : "FULL_DAY";
        AcademicYear ay = academicYearRepository.findByIsCurrentTrue().orElse(null);
        if (!accessControlService.isSchoolWideStaff() && ay != null) {
            Teacher teacher = accessControlService.requireCurrentTeacher();
            accessControlService.assertTeacherAssignedToSection(teacher.getId(), sectionId, ay.getId());
        }
        return attendanceRepository.findBySectionAndDateAndSession(sectionId, date, resolvedSession)
                .stream()
                .map(a -> toDto(a, a.getEnrollment().getRollNumber()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentAttendanceSummaryDto getStudentAttendanceSummary(Long studentId, Long academicYearId) {
        accessControlService.assertCanAccessStudent(studentId);
        List<Attendance> list = attendanceRepository.findByStudentAndYear(studentId, academicYearId);
        long total = list.size();
        long present = list.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long absent = list.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long late = list.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        long half = list.stream().filter(a -> "HALF_DAY".equals(a.getStatus())).count();
        long leave = list.stream().filter(a -> "APPROVED_LEAVE".equals(a.getStatus())).count();
        double effectivePresent = present + late + (half * 0.5);
        double pct = total == 0 ? 100.0 : Math.round((effectivePresent * 10000.0) / total) / 100.0;

        return StudentAttendanceSummaryDto.builder()
                .studentId(studentId)
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .halfDays(half)
                .leaveDays(leave)
                .attendancePercentage(pct)
                .records(list.stream().map(a -> toDto(a, null)).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getMonthlyReport(Long academicYearId, Long classId, Long sectionId, LocalDate from, LocalDate to) {
        Long yearId = academicYearId != null ? academicYearId
                : academicYearRepository.findByIsCurrentTrue().map(AcademicYear::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("No current academic year"));
        return attendanceRepository.findReportRecords(yearId, classId, sectionId, from, to).stream()
                .map(a -> toDto(a, null)).toList();
    }

    @Transactional
    public List<TeacherAttendanceDto> markTeacherAttendance(MarkTeacherAttendanceRequest req) {
        User marker = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        List<TeacherAttendanceDto> out = new ArrayList<>();
        for (TeacherAttendanceEntry e : req.getRecords()) {
            Teacher t = teacherRepository.findById(e.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + e.getTeacherId()));
            TeacherAttendance ta = teacherAttendanceRepository.findByTeacherIdAndAttendanceDate(t.getId(), req.getAttendanceDate())
                    .orElse(TeacherAttendance.builder().teacher(t).attendanceDate(req.getAttendanceDate()).build());
            ta.setStatus(e.getStatus().toUpperCase());
            ta.setRemarks(e.getRemarks());
            ta.setMarkedBy(marker);
            out.add(toTeacherDto(teacherAttendanceRepository.save(ta)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<TeacherAttendanceDto> getTeacherAttendance(LocalDate date, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return teacherAttendanceRepository.findBetween(from, to).stream().map(this::toTeacherDto).toList();
        }
        return teacherAttendanceRepository.findByDate(date != null ? date : LocalDate.now()).stream()
                .map(this::toTeacherDto).toList();
    }

    private void notifyParentsOfAbsence(Student student, LocalDate date, Section section) {
        String title = "Attendance Alert: " + student.getFullName();
        String msg = student.getFullName() + " (" + section.getSchoolClass().getName() + "-" + section.getName()
                + ") was marked ABSENT on " + date + ".";
        for (StudentParent sp : studentParentRepository.findByStudentIdWithParent(student.getId())) {
            notificationService.notifyUser(sp.getParent().getUser(), title, msg, "ABSENT_ALERT", "STUDENT", student.getId(), true);
        }
    }

    private AttendanceRecordDto toDto(Attendance a, String rollNo) {
        return AttendanceRecordDto.builder()
                .id(a.getId())
                .studentId(a.getStudent().getId())
                .admissionNumber(a.getStudent().getAdmissionNumber())
                .studentName(a.getStudent().getFullName())
                .rollNumber(rollNo)
                .className(a.getSchoolClass().getName())
                .sectionName(a.getSection().getName())
                .attendanceDate(a.getAttendanceDate())
                .session(a.getSession())
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .correctionReason(a.getCorrectionReason())
                .build();
    }

    private TeacherAttendanceDto toTeacherDto(TeacherAttendance ta) {
        return TeacherAttendanceDto.builder()
                .id(ta.getId())
                .teacherId(ta.getTeacher().getId())
                .employeeCode(ta.getTeacher().getEmployeeCode())
                .teacherName(ta.getTeacher().getFullName())
                .attendanceDate(ta.getAttendanceDate())
                .status(ta.getStatus())
                .remarks(ta.getRemarks())
                .build();
    }
}
