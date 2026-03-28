package com.qar.securitysystem.controller;

import com.qar.securitysystem.config.AppSecurityProperties;
import com.qar.securitysystem.dto.LoginRequest;
import com.qar.securitysystem.dto.RegisterRequest;
import com.qar.securitysystem.dto.UserResponse;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.security.AppPrincipal;
import com.qar.securitysystem.service.AuthService;
import com.qar.securitysystem.service.SessionService;
import com.qar.securitysystem.util.SecurityUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionService sessionService;
    private final AppSecurityProperties securityProperties;

    public AuthController(AuthService authService, SessionService sessionService, AppSecurityProperties securityProperties) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            UserEntity u = authService.register(req);
            return ResponseEntity.ok(toUserResponse(u));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        UserEntity u = authService.authenticate(req);
        if (u == null) {
            return ResponseEntity.status(401).body(error(401, "invalid_credentials"));
        }
        String token = sessionService.createSession(u);

        ResponseCookie cookie = ResponseCookie.from(securityProperties.getCookieName(), token)
                .httpOnly(true)
                .secure(securityProperties.isCookieSecure())
                .path("/")
                .sameSite(securityProperties.getCookieSameSite())
                .maxAge(Duration.ofMinutes(securityProperties.getSessionTtlMinutes()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(toUserResponse(u));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request) {
        String raw = readCookie(request, securityProperties.getCookieName());
        if (raw != null) {
            sessionService.revokeSessionToken(raw);
        }
        ResponseCookie cookie = ResponseCookie.from(securityProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(securityProperties.isCookieSecure())
                .path("/")
                .sameSite(securityProperties.getCookieSameSite())
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("code", 200, "message", "ok"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        AppPrincipal p = SecurityUtil.requirePrincipal(authentication);
        UserResponse resp = new UserResponse();
        resp.setId(p.getUserId());
        resp.setEmailOrUsername(p.getEmailOrUsername());
        resp.setRole(p.getRole().name().toLowerCase());
        return ResponseEntity.ok(resp);
    }

    private UserResponse toUserResponse(UserEntity u) {
        UserResponse resp = new UserResponse();
        resp.setId(u.getId());
        resp.setEmailOrUsername(u.getAccount());
        resp.setRole(u.getRole() == null ? null : u.getRole().name().toLowerCase());
        resp.setCreatedAt(u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        return resp;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", message);
        return m;
    }

    private static String readCookie(jakarta.servlet.http.HttpServletRequest request, String name) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
