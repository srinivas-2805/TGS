package com.tgs.school.teachers.repository;

import com.tgs.school.teachers.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);
    Optional<Teacher> findByEmployeeCode(String employeeCode);
    long countByDeletedAtIsNull();

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user WHERE t.deletedAt IS NULL AND (" +
           "LOWER(t.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.employeeCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(t.specialization,'')) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Teacher> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user WHERE t.deletedAt IS NULL")
    Page<Teacher> findAllActive(Pageable pageable);
}
