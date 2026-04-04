package com.qar.securitysystem.controller;

import com.qar.securitysystem.security.AppPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/")
    public void root(Authentication authentication, HttpServletResponse response) throws Exception {
        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal) {
            response.sendRedirect("/workbench");
            return;
        }
        response.sendRedirect("/auth");
    }

    @GetMapping("/auth")
    public ResponseEntity<Resource> auth() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/auth.html"));
    }

    @GetMapping("/workbench")
    public ResponseEntity<Resource> workbench() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/workbench.html"));
    }

    @GetMapping("/admin")
    public ResponseEntity<Resource> admin() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/admin.html"));
    }

    @GetMapping("/feedback")
    public ResponseEntity<Resource> feedback() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/feedback.html"));
    }
}
