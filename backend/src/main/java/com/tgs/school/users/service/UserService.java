package com.tgs.school.users.service;

import com.tgs.school.auth.entity.Role;
import com.tgs.school.auth.entity.RoleName;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.RefreshTokenRepository;
import com.tgs.school.auth.repository.RoleRepository;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.dto.PageResponse;
import com.tgs.school.common.exception.ConflictException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import com.tgs.school.users.dto.UserDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public User createUserAccount(String username, String email, String phone, String fullName, String rawPassword, Set<RoleName> roleNames) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists: " + email);
        }
        Set<Role> roles = new HashSet<>();
        for (RoleName rn : roleNames) {
            Role role = roleRepository.findByName(rn)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + rn));
            roles.add(role);
        }
        User user = User.builder()
                .username(username.trim())
                .email(email.trim().toLowerCase())
                .phone(phone)
                .fullName(fullName.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .roles(roles)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        User user = createUserAccount(req.getUsername(), req.getEmail(), req.getPhone(),
                req.getFullName(), req.getPassword(), req.getRoles());
        auditService.log("CREATE_USER", "USER", user.getId().toString(), "Created user " + user.getUsername());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String q, Pageable pageable) {
        Page<User> page = (q == null || q.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.searchUsers(q.trim(), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UpdateUserStatusRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(req.isEnabled());
        user.setAccountLocked(req.isAccountLocked());
        if (!req.isEnabled() || req.isAccountLocked()) {
            refreshTokenRepository.revokeAllForUser(user);
        }
        userRepository.save(user);
        auditService.log("UPDATE_USER_STATUS", "USER", userId.toString(),
                "enabled=" + req.isEnabled() + ", locked=" + req.isAccountLocked());
        return toResponse(user);
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phone(u.getPhone())
                .fullName(u.getFullName())
                .enabled(u.isEnabled())
                .accountLocked(u.isAccountLocked())
                .lastLoginAt(u.getLastLoginAt())
                .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}
