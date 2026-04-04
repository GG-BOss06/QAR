package com.qar.securitysystem.controller;

import com.qar.securitysystem.dto.FeedbackCreateRequest;
import com.qar.securitysystem.service.FeedbackService;
import com.qar.securitysystem.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @RequestBody FeedbackCreateRequest req) {
        try {
            String ownerId = SecurityUtil.requirePrincipal(authentication).getUserId();
            return ResponseEntity.ok(feedbackService.create(ownerId, req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> mine(Authentication authentication) {
        String ownerId = SecurityUtil.requirePrincipal(authentication).getUserId();
        return ResponseEntity.ok(feedbackService.listMine(ownerId));
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", message);
        return m;
    }
}

