package com.qar.securitysystem.service;

import com.qar.securitysystem.config.AdminProperties;
import com.qar.securitysystem.dto.LoginRequest;
import com.qar.securitysystem.dto.RegisterRequest;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.model.UserRole;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    public UserEntity register(RegisterRequest req) {
        String username = normalize(req.getEmailOrUsername());
        if (username.isBlank()) {
            throw new IllegalArgumentException("emailOrUsername_required");
        }
        if (adminProperties.getUsername() != null && username.equalsIgnoreCase(adminProperties.getUsername())) {
            throw new IllegalArgumentException("admin_already_exists");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("password_required");
        }
        if (req.getPasswordConfirm() == null || !req.getPasswordConfirm().equals(req.getPassword())) {
            throw new IllegalArgumentException("password_confirm_mismatch");
        }
        if (userRepository.existsByAccount(username)) {
            throw new IllegalArgumentException("user_already_exists");
        }

        UserEntity u = new UserEntity();
        u.setId(IdUtil.newId());
        u.setAccount(username);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(UserRole.USER);
        u.setCreatedAt(Instant.now());
        return userRepository.save(u);
    }

    public UserEntity authenticate(LoginRequest req) {
        String username = normalize(req.getEmailOrUsername());
        if (username.isBlank() || req.getPassword() == null) {
            return null;
        }
        UserEntity u = userRepository.findByAccount(username).orElse(null);
        if (u == null) {
            return null;
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            return null;
        }
        return u;
    }

    private static String normalize(String v) {
        if (v == null) {
            return "";
        }
        return v.trim();
    }
}
