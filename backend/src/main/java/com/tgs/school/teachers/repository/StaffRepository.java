package com.tgs.school.teachers.repository;

import com.tgs.school.teachers.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Page<Staff> findByDeletedAtIsNull(Pageable pageable);
    long countByDeletedAtIsNull();
    boolean existsByEmployeeCode(String employeeCode);
}
