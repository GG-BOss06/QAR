package com.qar.securitysystem.service;

import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.dto.FeedbackResponse;
import com.qar.securitysystem.dto.UserResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.FeedbackEntity;
import com.qar.securitysystem.model.FeedbackStatus;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.FeedbackRepository;
import com.qar.securitysystem.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final FileRecordRepository fileRecordRepository;
    private final FeedbackRepository feedbackRepository;

    public AdminService(UserRepository userRepository, FileRecordRepository fileRecordRepository, FeedbackRepository feedbackRepository) {
        this.userRepository = userRepository;
        this.fileRecordRepository = fileRecordRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    public List<FileRecordResponse> listAllFiles() {
        return fileRecordRepository.findAll().stream().map(this::toFileResponse).toList();
    }

    public List<FileRecordEntity> listAllFileEntities() {
        return fileRecordRepository.findAll();
    }

    public List<FeedbackResponse> listAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toFeedbackResponse).toList();
    }

    public FeedbackResponse updateFeedback(String id, String status, String adminReply) {
        FeedbackEntity e = feedbackRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not_found"));
        boolean touched = false;
        if (status != null && !status.isBlank()) {
            FeedbackStatus s = parseStatus(status);
            e.setStatus(s);
            touched = true;
        }
        if (adminReply != null) {
            String r = adminReply.trim();
            if (r.length() > 8000) {
                r = r.substring(0, 8000);
            }
            e.setAdminReply(r.isBlank() ? null : r);
            e.setRepliedAt(r.isBlank() ? null : Instant.now());
            touched = true;
        }
        if (touched) {
            e.setUpdatedAt(Instant.now());
            feedbackRepository.save(e);
        }
        return toFeedbackResponse(e);
    }

    private UserResponse toUserResponse(UserEntity u) {
        UserResponse resp = new UserResponse();
        resp.setId(u.getId());
        resp.setEmailOrUsername(u.getAccount());
        resp.setRole(u.getRole() == null ? null : u.getRole().name().toLowerCase());
        resp.setCreatedAt(u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        return resp;
    }

    private FileRecordResponse toFileResponse(FileRecordEntity r) {
        FileRecordResponse resp = new FileRecordResponse();
        resp.setId(r.getId());
        resp.setOwnerId(r.getOwnerId());
        resp.setOriginalName(r.getOriginalName());
        resp.setContentType(r.getContentType());
        resp.setSizeBytes(r.getSizeBytes());
        resp.setPolicy(r.getPolicy());
        resp.setWrappedKey(r.getWrappedKey());
        resp.setCreatedAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        return resp;
    }

    private FeedbackResponse toFeedbackResponse(FeedbackEntity e) {
        return com.qar.securitysystem.service.FeedbackService.toResponse(e);
    }

    private FeedbackStatus parseStatus(String s) {
        String v = s.trim().toUpperCase().replace("-", "_");
        try {
            return FeedbackStatus.valueOf(v);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid_status");
        }
    }
}
