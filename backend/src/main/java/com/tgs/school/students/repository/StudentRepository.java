package com.tgs.school.students.repository;

import com.tgs.school.students.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByAdmissionNumber(String admissionNumber);
    boolean existsByAdmissionNumber(String admissionNumber);
    long countByDeletedAtIsNullAndStatus(String status);
    List<Student> findTop5ByDeletedAtIsNullOrderByCreatedAtDesc();

    @Query("SELECT MAX(s.id) FROM Student s")
    Optional<Long> findMaxId();
}
