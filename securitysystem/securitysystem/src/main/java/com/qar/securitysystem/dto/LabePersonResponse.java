package com.qar.securitysystem.dto;

import java.util.List;

public class LabePersonResponse {
    private String id;
    private String userId;
    private String personNo;
    private String fullName;
    private String department;
    private String airline;
    private String positionTitle;
    private String personCategory;
    private String dutyDomain;
    private String fleetGroup;
    private String clearanceLevel;
    private String phone;
    private String createdAt;
    private boolean accountReady;
    private boolean secretBundleReady;
    private boolean accessEnabled;
    private String accessStatus;
    private String accessRevokedAt;
    private String accessRevokedReason;
    private Long secretBundleVersion;
    private String secretBundleStatus;
    private String secretBundleIssuedAt;
    private String secretBundleIssuedReason;
    private String secretBundleAttributeDigest;
    private List<String> attributes;
    private List<String> authorities;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPersonNo() {
        return personNo;
    }

    public void setPersonNo(String personNo) {
        this.personNo = personNo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAccountReady() {
        return accountReady;
    }

    public void setAccountReady(boolean accountReady) {
        this.accountReady = accountReady;
    }

    public boolean isSecretBundleReady() {
        return secretBundleReady;
    }

    public void setSecretBundleReady(boolean secretBundleReady) {
        this.secretBundleReady = secretBundleReady;
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

    public Long getSecretBundleVersion() {
        return secretBundleVersion;
    }

    public void setSecretBundleVersion(Long secretBundleVersion) {
        this.secretBundleVersion = secretBundleVersion;
    }

    public String getSecretBundleStatus() {
        return secretBundleStatus;
    }

    public void setSecretBundleStatus(String secretBundleStatus) {
        this.secretBundleStatus = secretBundleStatus;
    }

    public String getSecretBundleIssuedAt() {
        return secretBundleIssuedAt;
    }

    public void setSecretBundleIssuedAt(String secretBundleIssuedAt) {
        this.secretBundleIssuedAt = secretBundleIssuedAt;
    }

    public String getSecretBundleIssuedReason() {
        return secretBundleIssuedReason;
    }

    public void setSecretBundleIssuedReason(String secretBundleIssuedReason) {
        this.secretBundleIssuedReason = secretBundleIssuedReason;
    }

    public String getSecretBundleAttributeDigest() {
        return secretBundleAttributeDigest;
    }

    public void setSecretBundleAttributeDigest(String secretBundleAttributeDigest) {
        this.secretBundleAttributeDigest = secretBundleAttributeDigest;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}
