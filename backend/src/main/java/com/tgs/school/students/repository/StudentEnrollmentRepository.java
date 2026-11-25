package com.tgs.school.students.repository;

import com.tgs.school.students.entity.StudentEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    @Query("SELECT e FROM StudentEnrollment e " +
           "JOIN FETCH e.student s " +
           "JOIN FETCH e.academicYear " +
           "JOIN FETCH e.schoolClass " +
           "JOIN FETCH e.section " +
           "WHERE e.student.id = :studentId AND e.academicYear.id = :yearId")
    Optional<StudentEnrollment> findByStudentAndYear(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("SELECT e FROM StudentEnrollment e " +
           "JOIN FETCH e.academicYear " +
           "JOIN FETCH e.schoolClass " +
           "JOIN FETCH e.section " +
           "WHERE e.student.id = :studentId ORDER BY e.academicYear.startDate DESC")
    List<StudentEnrollment> findHistoryByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT e FROM StudentEnrollment e " +
           "JOIN FETCH e.student s " +
           "JOIN FETCH e.academicYear " +
           "JOIN FETCH e.schoolClass c " +
           "JOIN FETCH e.section sec " +
           "WHERE s.deletedAt IS NULL " +
           "AND (:yearId IS NULL OR e.academicYear.id = :yearId) " +
           "AND (:classId IS NULL OR c.id = :classId) " +
           "AND (:sectionId IS NULL OR sec.id = :sectionId) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:q IS NULL OR :q = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<StudentEnrollment> searchEnrollments(
            @Param("yearId") Long yearId,
            @Param("classId") Long classId,
            @Param("sectionId") Long sectionId,
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable);

    @Query("SELECT e FROM StudentEnrollment e JOIN FETCH e.student s WHERE e.academicYear.id = :yearId AND e.section.id = :sectionId AND s.deletedAt IS NULL AND e.enrollmentStatus = 'ACTIVE' ORDER BY e.rollNumber, s.fullName")
    List<StudentEnrollment> findActiveByYearAndSection(@Param("yearId") Long yearId, @Param("sectionId") Long sectionId);

    @Query("SELECT e FROM StudentEnrollment e JOIN FETCH e.student s WHERE e.academicYear.id = :yearId AND e.schoolClass.id = :classId AND s.deletedAt IS NULL AND e.enrollmentStatus = 'ACTIVE'")
    List<StudentEnrollment> findActiveByYearAndClass(@Param("yearId") Long yearId, @Param("classId") Long classId);

    @Query("SELECT c.name, COUNT(e.id) FROM StudentEnrollment e JOIN e.schoolClass c JOIN e.student s WHERE e.academicYear.id = :yearId AND s.deletedAt IS NULL AND e.enrollmentStatus = 'ACTIVE' GROUP BY c.id, c.name, c.levelOrder ORDER BY c.levelOrder")
    List<Object[]> countStudentsByClassForYear(@Param("yearId") Long yearId);

    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    long countBySectionIdAndAcademicYearIdAndEnrollmentStatus(Long sectionId, Long academicYearId, String status);
}
