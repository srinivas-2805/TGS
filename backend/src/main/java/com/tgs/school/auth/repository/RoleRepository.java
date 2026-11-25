package com.tgs.school.auth.repository;

import com.tgs.school.auth.entity.Role;
import com.tgs.school.auth.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
