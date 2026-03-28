package com.qar.securitysystem.service;

import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.dto.UserResponse;
import com.qar.securitysystem.model.FileRecordEntity;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.repo.FileRecordRepository;
import com.qar.securitysystem.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final FileRecordRepository fileRecordRepository;

    public AdminService(UserRepository userRepository, FileRecordRepository fileRecordRepository) {
        this.userRepository = userRepository;
        this.fileRecordRepository = fileRecordRepository;
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
}
