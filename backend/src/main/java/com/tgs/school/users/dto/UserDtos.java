package com.tgs.school.users.dto;

import com.tgs.school.auth.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

public class UserDtos {

    @Data
    public static class CreateUserRequest {
        @NotBlank @Size(min = 3, max = 80)
        private String username;
        @NotBlank @Email
        private String email;
        private String phone;
        @NotBlank
        private String fullName;
        @NotBlank @Size(min = 8)
        private String password;
        @NotEmpty
        private Set<RoleName> roles;
    }

    @Data
    public static class UpdateUserStatusRequest {
        private boolean enabled;
        private boolean accountLocked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String phone;
        private String fullName;
        private boolean enabled;
        private boolean accountLocked;
        private Instant lastLoginAt;
        private Set<String> roles;
    }
}
