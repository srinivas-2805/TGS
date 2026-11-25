package com.tgs.school.parents.repository;

import com.tgs.school.parents.entity.ParentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParentRepository extends JpaRepository<ParentProfile, Long> {
    Optional<ParentProfile> findByUserId(Long userId);
    Optional<ParentProfile> findByEmailAndDeletedAtIsNull(String email);
    Optional<ParentProfile> findByPhoneAndDeletedAtIsNull(String phone);

    @Query("SELECT p FROM ParentProfile p JOIN FETCH p.user WHERE p.deletedAt IS NULL AND (" +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.phone) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ParentProfile> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM ParentProfile p JOIN FETCH p.user WHERE p.deletedAt IS NULL")
    Page<ParentProfile> findAllActive(Pageable pageable);
}
