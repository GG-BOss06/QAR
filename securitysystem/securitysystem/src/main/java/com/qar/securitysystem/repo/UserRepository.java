package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByAccount(String account);
    boolean existsByAccount(String account);
}
