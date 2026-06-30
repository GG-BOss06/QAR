package com.qar.securitysystem.dto;

import java.util.List;

public class UserResponse {
    private String id;
    private String emailOrUsername;
    private String role;
    private String createdAt;
    private String fullName;
    private String personNo;
    private String department;
    private String airline;
    private String positionTitle;
    private String personCategory;
    private String dutyDomain;
    private String fleetGroup;
    private String clearanceLevel;
    private boolean accessEnabled;
    private String accessStatus;
    private String accessRevokedAt;
    private String accessRevokedReason;
    private List<String> attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmailOrUsername() {
        return emailOrUsername;
    }

    public void setEmailOrUsername(String emailOrUsername) {
        this.emailOrUsername = emailOrUsername;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPersonNo() {
        return personNo;
    }

    public void setPersonNo(String personNo) {
        this.personNo = personNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public String getPersonCategory() {
        return personCategory;
    }

    public void setPersonCategory(String personCategory) {
        this.personCategory = personCategory;
    }

    public String getDutyDomain() {
        return dutyDomain;
    }

    public void setDutyDomain(String dutyDomain) {
        this.dutyDomain = dutyDomain;
    }

    public String getFleetGroup() {
        return fleetGroup;
    }

    public void setFleetGroup(String fleetGroup) {
        this.fleetGroup = fleetGroup;
    }

    public String getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(String clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    public boolean isAccessEnabled() {
        return accessEnabled;
    }

    public void setAccessEnabled(boolean accessEnabled) {
        this.accessEnabled = accessEnabled;
    }

    public String getAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(String accessStatus) {
        this.accessStatus = accessStatus;
    }

    public String getAccessRevokedAt() {
        return accessRevokedAt;
    }

    public void setAccessRevokedAt(String accessRevokedAt) {
        this.accessRevokedAt = accessRevokedAt;
    }

    public String getAccessRevokedReason() {
        return accessRevokedReason;
    }

    public void setAccessRevokedReason(String accessRevokedReason) {
        this.accessRevokedReason = accessRevokedReason;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
}
