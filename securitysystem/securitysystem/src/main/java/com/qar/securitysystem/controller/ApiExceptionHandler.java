package com.qar.securitysystem.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final HttpClient DEBUG_HTTP = HttpClient.newHttpClient();

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e, jakarta.servlet.http.HttpServletRequest request) {
        // #region debug-point D:illegal-argument
        debugReport("D", "ApiExceptionHandler.handleIllegalArgument", "[DEBUG] request failed with 400", Map.of(
                "path", pathOf(request),
                "method", methodOf(request),
                "message", safeMessage(e, "bad_request")
        ));
        // #endregion
        return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", safeMessage(e, "bad_request")
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e, jakarta.servlet.http.HttpServletRequest request) {
        // #region debug-point D:data-integrity
        debugReport("D", "ApiExceptionHandler.handleDataIntegrity", "[DEBUG] data integrity failed with 400", Map.of(
                "path", pathOf(request),
                "method", methodOf(request),
                "message", "data_too_large_or_invalid"
        ));
        // #endregion
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", 400,
                "message", "data_too_large_or_invalid"
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e, jakarta.servlet.http.HttpServletRequest request) {
        // #region debug-point D:access-denied
        debugReport("D", "ApiExceptionHandler.handleAccessDenied", "[DEBUG] request denied with 403", Map.of(
                "path", pathOf(request),
                "method", methodOf(request),
                "message", safeMessage(e, "forbidden")
        ));
        // #endregion
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", 403,
                "message", safeMessage(e, "forbidden")
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e, jakarta.servlet.http.HttpServletRequest request) {
        // #region debug-point D:other-exception
        debugReport("D", "ApiExceptionHandler.handleOther", "[DEBUG] request failed with 500", Map.of(
                "path", pathOf(request),
                "method", methodOf(request),
                "message", safeMessage(e, "request_failed"),
                "type", e == null ? "" : e.getClass().getName()
        ));
        // #endregion
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", safeMessage(e, "request_failed")
        ));
    }

    private static String safeMessage(Exception e, String fallback) {
        String message = e == null ? null : e.getMessage();
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    private static String pathOf(jakarta.servlet.http.HttpServletRequest request) {
        return request == null || request.getRequestURI() == null ? "" : request.getRequestURI();
    }

    private static String methodOf(jakarta.servlet.http.HttpServletRequest request) {
        return request == null || request.getMethod() == null ? "" : request.getMethod();
    }

    private static void debugReport(String hypothesisId, String location, String msg, Map<String, Object> data) {
        try {
            Path envPath = Path.of(".dbg", "zhangsan-data-zero.env");
            String url = "http://127.0.0.1:7777/event";
            String sessionId = "zhangsan-data-zero";
            if (Files.exists(envPath)) {
                String env = Files.readString(envPath, StandardCharsets.UTF_8);
                for (String line : env.split("\\R")) {
                    if (line.startsWith("DEBUG_SERVER_URL=")) {
                        url = line.substring("DEBUG_SERVER_URL=".length()).trim();
                    } else if (line.startsWith("DEBUG_SESSION_ID=")) {
                        sessionId = line.substring("DEBUG_SESSION_ID=".length()).trim();
                    }
                }
            }
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                    "sessionId", sessionId,
                    "runId", "pre-fix",
                    "hypothesisId", hypothesisId,
                    "location", location,
                    "msg", msg,
                    "data", data,
                    "ts", System.currentTimeMillis()
            ));
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            DEBUG_HTTP.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}
