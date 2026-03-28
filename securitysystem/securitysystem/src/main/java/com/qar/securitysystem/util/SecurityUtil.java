package com.qar.securitysystem.util;

import com.qar.securitysystem.security.AppPrincipal;
import org.springframework.security.core.Authentication;

public final class SecurityUtil {
    private SecurityUtil() {
    }

    public static AppPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal)) {
            throw new IllegalStateException("unauthorized");
        }
        return (AppPrincipal) authentication.getPrincipal();
    }
}

