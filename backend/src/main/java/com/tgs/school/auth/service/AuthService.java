package com.tgs.school.auth.service;

import com.tgs.school.auth.dto.AuthDtos.*;
import com.tgs.school.auth.entity.PasswordResetToken;
import com.tgs.school.auth.entity.RefreshToken;
import com.tgs.school.auth.entity.User;
import com.tgs.school.auth.repository.PasswordResetTokenRepository;
import com.tgs.school.auth.repository.RefreshTokenRepository;
import com.tgs.school.auth.repository.UserRepository;
import com.tgs.school.auth.security.CustomUserDetails;
import com.tgs.school.auth.security.JwtService;
import com.tgs.school.auth.security.SecurityUtils;
import com.tgs.school.common.audit.AuditService;
import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtService.sha256(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .build());

        auditService.log("LOGIN", "USER", user.getId().toString(), "User logged in: " + user.getUsername());
        return buildAuthResponse(user, accessToken, rawRefreshToken);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = JwtService.sha256(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired or revoked");
        }
        User user = stored.getUser();
        if (!user.isEnabled() || user.isAccountLocked() || user.getDeletedAt() != null) {
            throw new BadRequestException("User account is disabled or locked");
        }
        // Rotate refresh token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        CustomUserDetails principal = new CustomUserDetails(user);
        String newAccess = jwtService.generateAccessToken(principal);
        String newRefresh = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(JwtService.sha256(newRefresh))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .build());

        return buildAuthResponse(user, newAccess, newRefresh);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenRepository.findByTokenHash(JwtService.sha256(request.getRefreshToken()))
                    .ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
        }
        Long uid = SecurityUtils.getCurrentUserIdOrNull();
        if (uid != null) {
            userRepository.findById(uid).ifPresent(refreshTokenRepository::revokeAllForUser);
        }
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail().trim());
        if (userOpt.isEmpty()) {
            // Do not reveal user existence
            return null;
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());
        log.info("Password reset token generated for user {} (email configured via MAIL_ENABLED)", user.getUsername());
        auditService.log("FORGOT_PASSWORD_REQUEST", "USER", user.getId().toString(), "Password reset requested");
        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid password reset token"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token is expired or already used");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenRepository.revokeAllForUser(user);
        auditService.log("RESET_PASSWORD", "USER", user.getId().toString(), "Password reset completed");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user);
        auditService.log("CHANGE_PASSWORD", "USER", user.getId().toString(), "User changed password");
    }

    public UserProfile getCurrentProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserProfile(user);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtService.getAccessExpirationMs())
                .user(toUserProfile(user))
                .build();
    }

    public static UserProfile toUserProfile(User user) {
        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}
