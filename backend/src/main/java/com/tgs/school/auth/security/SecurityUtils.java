package com.tgs.school.auth.security;

import com.tgs.school.auth.entity.RoleName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            throw new AccessDeniedException("Authentication required");
        }
        return details;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }

    public static String getCurrentUsernameOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUsername();
        }
        return "SYSTEM";
    }

    public static boolean hasAnyRole(RoleName... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            return false;
        }
        Set<String> userRoles = details.getRoleNames();
        for (RoleName role : roles) {
            if (userRoles.contains(role.name())) {
                return true;
            }
        }
        return false;
    }
}
