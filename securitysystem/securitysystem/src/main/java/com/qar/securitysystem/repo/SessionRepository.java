package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    Optional<SessionEntity> findFirstByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);
}

