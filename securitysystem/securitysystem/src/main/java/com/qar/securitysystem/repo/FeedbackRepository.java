package com.qar.securitysystem.repo;

import com.qar.securitysystem.model.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, String> {
    List<FeedbackEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<FeedbackEntity> findAllByOrderByCreatedAtDesc();
}

