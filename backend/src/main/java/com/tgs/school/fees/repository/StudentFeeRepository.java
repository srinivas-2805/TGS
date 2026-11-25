package com.tgs.school.fees.repository;

import com.tgs.school.fees.entity.StudentFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {
    @Query("SELECT sf FROM StudentFee sf JOIN FETCH sf.student JOIN FETCH sf.feeStructure fs JOIN FETCH fs.schoolClass JOIN FETCH sf.academicYear " +
           "WHERE sf.student.id = :studentId ORDER BY sf.createdAt DESC")
    List<StudentFee> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT sf FROM StudentFee sf JOIN FETCH sf.student s JOIN FETCH sf.feeStructure fs JOIN FETCH fs.schoolClass " +
           "WHERE (:yearId IS NULL OR sf.academicYear.id = :yearId) AND (:status IS NULL OR sf.status = :status) " +
           "ORDER BY sf.updatedAt DESC")
    List<StudentFee> findFiltered(@Param("yearId") Long yearId, @Param("status") String status);

    Optional<StudentFee> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    @Query("SELECT COALESCE(SUM(sf.paidAmount), 0) FROM StudentFee sf WHERE (:yearId IS NULL OR sf.academicYear.id = :yearId)")
    BigDecimal sumTotalCollected(@Param("yearId") Long yearId);

    @Query("SELECT COALESCE(SUM(sf.balanceAmount), 0) FROM StudentFee sf WHERE (:yearId IS NULL OR sf.academicYear.id = :yearId) AND sf.status IN ('PENDING','PARTIAL','OVERDUE')")
    BigDecimal sumTotalPending(@Param("yearId") Long yearId);
}
