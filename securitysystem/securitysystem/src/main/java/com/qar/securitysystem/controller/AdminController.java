package com.qar.securitysystem.controller;

import com.qar.securitysystem.dto.AdminFeedbackUpdateRequest;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.service.AdminService;
import com.qar.securitysystem.service.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final FileService fileService;

    public AdminController(AdminService adminService, FileService fileService) {
        this.adminService = adminService;
        this.fileService = fileService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @GetMapping("/files")
    public ResponseEntity<?> files() {
        return ResponseEntity.ok(adminService.listAllFiles());
    }

    @GetMapping("/files/export")
    public ResponseEntity<byte[]> exportAll() {
        List<FileRecordEntity> all = adminService.listAllFileEntities();
        byte[] zip = zipAll(all);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"all-data.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    @GetMapping("/feedback")
    public ResponseEntity<?> feedback() {
        return ResponseEntity.ok(adminService.listAllFeedback());
    }

    @PatchMapping("/feedback/{id}")
    public ResponseEntity<?> updateFeedback(@PathVariable("id") String id, @RequestBody AdminFeedbackUpdateRequest req) {
        try {
            return ResponseEntity.ok(adminService.updateFeedback(id, req == null ? null : req.getStatus(), req == null ? null : req.getAdminReply()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("code", 400, "message", e.getMessage()));
        }
    }

    private byte[] zipAll(List<FileRecordEntity> all) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(bos);
            for (FileRecordEntity r : all) {
                byte[] raw = fileService.decryptForDownload(r);
                String entryName = r.getOwnerId() + "/" + r.getId() + "_" + safeFilename(r.getOriginalName());
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(raw);
                zos.closeEntry();
            }
            zos.finish();
            zos.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("export_failed", e);
        }
    }

    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "download.bin";
        }
        return name.replace("\\", "_").replace("/", "_").replace("\n", " ").replace("\r", " ");
    }
}
