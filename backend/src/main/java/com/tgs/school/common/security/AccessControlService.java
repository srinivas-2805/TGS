package com.tgs.school.common.security;

import com.tgs.school.academics.entity.AcademicYear;
import com.tgs.school.academics.repository.AcademicYearRepository;
import com.tgs.school.academics.repository.SectionRepository;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.parents.entity.ParentProfile;
import com.tgs.school.parents.repository.ParentRepository;
import com.tgs.school.students.entity.Student;
import com.tgs.school.students.entity.StudentEnrollment;
import com.tgs.school.students.repository.StudentEnrollmentRepository;
import com.tgs.school.students.repository.StudentParentRepository;
import com.tgs.school.students.repository.StudentRepository;
import com.tgs.school.teachers.entity.Teacher;
import com.tgs.school.teachers.repository.TeacherAssignmentRepository;
import com.tgs.school.teachers.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final StudentRepository studentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;

    public boolean isSchoolWideStaff() {
        return SecurityUtils.hasAnyRole(RoleName.SUPER_ADMIN, RoleName.PRINCIPAL, RoleName.ADMIN);
    }

    public void assertCanAccessStudent(Long studentId) {
        if (SecurityUtils.hasAnyRole(RoleName.SUPER_ADMIN, RoleName.PRINCIPAL, RoleName.ADMIN, RoleName.ACCOUNTANT, RoleName.LIBRARIAN)) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (SecurityUtils.hasAnyRole(RoleName.STUDENT) && student.getUser().getId().equals(userId)) {
            return;
        }
        if (SecurityUtils.hasAnyRole(RoleName.PARENT) && studentParentRepository.existsByStudentIdAndParentUserId(studentId, userId)) {
            return;
        }
        if (SecurityUtils.hasAnyRole(RoleName.TEACHER)) {
            Teacher teacher = teacherRepository.findByUserId(userId)
                    .orElseThrow(() -> new AccessDeniedException("Teacher profile not found"));
            AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);
            if (currentYear != null) {
                StudentEnrollment enrollment = enrollmentRepository.findByStudentAndYear(studentId, currentYear.getId()).orElse(null);
                if (enrollment != null) {
                    Long secId = enrollment.getSection().getId();
                    boolean teachesSection = teacherAssignmentRepository.existsByTeacherIdAndSectionIdAndAcademicYearId(
                            teacher.getId(), secId, currentYear.getId());
                    boolean isClassTeacher = sectionRepository.findById(secId)
                            .map(s -> s.getClassTeacher() != null && s.getClassTeacher().getId().equals(teacher.getId()))
                            .orElse(false);
                    if (teachesSection || isClassTeacher) {
                        return;
                    }
                }
            }
        }
        throw new AccessDeniedException("You do not have permission to access this student's records.");
    }

    public void assertCanAccessStudentAcademicsOrFees(Long studentId) {
        if (SecurityUtils.hasAnyRole(RoleName.SUPER_ADMIN, RoleName.PRINCIPAL, RoleName.ADMIN)) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (SecurityUtils.hasAnyRole(RoleName.STUDENT) && student.getUser().getId().equals(userId)) {
            return;
        }
        if (SecurityUtils.hasAnyRole(RoleName.PARENT) && studentParentRepository.existsByStudentIdAndParentUserId(studentId, userId)) {
            return;
        }
        if (SecurityUtils.hasAnyRole(RoleName.ACCOUNTANT)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access this student's fee records.");
    }

    public Teacher requireCurrentTeacher() {
        Long userId = SecurityUtils.getCurrentUserId();
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Teacher profile not found for current user"));
    }

    public ParentProfile requireCurrentParent() {
        Long userId = SecurityUtils.getCurrentUserId();
        return parentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Parent profile not found for current user"));
    }

    public Student requireCurrentStudent() {
        Long userId = SecurityUtils.getCurrentUserId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Student profile not found for current user"));
    }

    public void assertTeacherAssignedToSection(Long teacherId, Long sectionId, Long academicYearId) {
        if (isSchoolWideStaff()) return;
        boolean assigned = teacherAssignmentRepository.existsByTeacherIdAndSectionIdAndAcademicYearId(teacherId, sectionId, academicYearId);
        boolean isClassTeacher = sectionRepository.findById(sectionId)
                .map(s -> s.getClassTeacher() != null && s.getClassTeacher().getId().equals(teacherId))
                .orElse(false);
        if (!assigned && !isClassTeacher) {
            throw new AccessDeniedException("Teacher is not assigned to this class section.");
        }
    }

    public void assertTeacherAssignedToSubject(Long teacherId, Long sectionId, Long subjectId, Long academicYearId) {
        if (isSchoolWideStaff()) return;
        boolean assigned = teacherAssignmentRepository.existsByTeacherIdAndSectionIdAndSubjectIdAndAcademicYearId(
                teacherId, sectionId, subjectId, academicYearId);
        if (!assigned) {
            throw new AccessDeniedException("Teacher is not assigned to this subject in the selected section.");
        }
    }
}
