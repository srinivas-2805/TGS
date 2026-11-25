package com.tgs.school.auth.repository;

import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (" +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.deletedAt IS NULL AND u.enabled = true AND r.name = :roleName")
    List<User> findActiveByRole(@Param("roleName") RoleName roleName);
}
