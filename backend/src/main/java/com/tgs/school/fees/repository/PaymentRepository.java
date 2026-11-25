package com.tgs.school.fees.repository;

import com.tgs.school.fees.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p JOIN FETCH p.student JOIN FETCH p.studentFee sf JOIN FETCH sf.feeStructure " +
           "WHERE p.student.id = :studentId ORDER BY p.paidAt DESC")
    List<Payment> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.student JOIN FETCH p.studentFee sf JOIN FETCH sf.feeStructure " +
           "ORDER BY p.paidAt DESC")
    List<Payment> findRecentPayments();
}
