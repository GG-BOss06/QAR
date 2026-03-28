package com.qar.securitysystem.service;

import com.qar.securitysystem.config.AppSecurityProperties;
import com.qar.securitysystem.model.SessionEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.SessionRepository;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.util.HashUtil;
import com.qar.securitysystem.util.IdUtil;
import com.qar.securitysystem.util.SecureTokenUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AppSecurityProperties securityProperties;

    public SessionService(SessionRepository sessionRepository, UserRepository userRepository, AppSecurityProperties securityProperties) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
    }

    public String createSession(UserEntity user) {
        String token = SecureTokenUtil.newTokenUrlSafe(32);
        String tokenHash = HashUtil.sha256Hex(token);

        SessionEntity s = new SessionEntity();
        s.setId(IdUtil.newId());
        s.setUserId(user.getId());
        s.setTokenHash(tokenHash);
        s.setCreatedAt(Instant.now());
        s.setExpiresAt(Instant.now().plus(securityProperties.getSessionTtlMinutes(), ChronoUnit.MINUTES));

        sessionRepository.save(s);
        return token;
    }

    public void revokeSessionToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = HashUtil.sha256Hex(rawToken);
        sessionRepository.findFirstByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, Instant.now())
                .ifPresent(s -> {
                    s.setRevokedAt(Instant.now());
                    sessionRepository.save(s);
                });
    }

    public UserEntity resolveUserFromSessionToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String tokenHash = HashUtil.sha256Hex(rawToken);
        SessionEntity s = sessionRepository.findFirstByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, Instant.now())
                .orElse(null);
        if (s == null) {
            return null;
        }
        return userRepository.findById(s.getUserId()).orElse(null);
    }
}

