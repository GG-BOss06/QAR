package com.qar.securitysystem.service;

import com.qar.securitysystem.dto.FeedbackCreateRequest;
import com.qar.securitysystem.dto.FeedbackResponse;
import com.qar.securitysystem.model.FeedbackEntity;
import com.qar.securitysystem.model.FeedbackStatus;
import com.qar.securitysystem.repo.FeedbackRepository;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public FeedbackResponse create(String ownerId, FeedbackCreateRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("invalid_request");
        }
        String message = req.getMessage() == null ? "" : req.getMessage().trim();
        if (message.isBlank()) {
            throw new IllegalArgumentException("message_required");
        }
        if (message.length() > 8000) {
            throw new IllegalArgumentException("message_too_long");
        }
        String subject = req.getSubject() == null ? "" : req.getSubject().trim();
        if (subject.length() > 140) {
            subject = subject.substring(0, 140);
        }
        String contact = req.getContact() == null ? null : req.getContact().trim();
        if (contact != null && contact.length() > 140) {
            contact = contact.substring(0, 140);
        }
        String type = req.getType() == null ? null : req.getType().trim();
        if (type != null && type.length() > 40) {
            type = type.substring(0, 40);
        }
        String relatedFileId = req.getRelatedFileId() == null ? null : req.getRelatedFileId().trim();
        if (relatedFileId != null && relatedFileId.length() > 64) {
            relatedFileId = relatedFileId.substring(0, 64);
        }

        Instant now = Instant.now();
        FeedbackEntity e = new FeedbackEntity();
        e.setId(IdUtil.newId());
        e.setOwnerId(ownerId);
        e.setType(type);
        e.setSubject(subject.isBlank() ? null : subject);
        e.setMessage(message);
        e.setContact(contact == null || contact.isBlank() ? null : contact);
        e.setRelatedFileId(relatedFileId == null || relatedFileId.isBlank() ? null : relatedFileId);
        e.setStatus(FeedbackStatus.NEW);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        feedbackRepository.save(e);
        return toResponse(e);
    }

    public List<FeedbackResponse> listMine(String ownerId) {
        return feedbackRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(FeedbackService::toResponse).toList();
    }

    public static FeedbackResponse toResponse(FeedbackEntity e) {
        FeedbackResponse r = new FeedbackResponse();
        r.setId(e.getId());
        r.setOwnerId(e.getOwnerId());
        r.setType(e.getType());
        r.setSubject(e.getSubject());
        r.setMessage(e.getMessage());
        r.setContact(e.getContact());
        r.setRelatedFileId(e.getRelatedFileId());
        r.setStatus(e.getStatus() == null ? null : e.getStatus().name().toLowerCase());
        r.setAdminReply(e.getAdminReply());
        r.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        r.setUpdatedAt(e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString());
        r.setRepliedAt(e.getRepliedAt() == null ? null : e.getRepliedAt().toString());
        return r;
    }
}
