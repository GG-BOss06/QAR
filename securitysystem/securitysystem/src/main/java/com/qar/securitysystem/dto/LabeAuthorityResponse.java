package com.qar.securitysystem.dto;

import java.util.List;

public class LabeAuthorityResponse {
    private String authorityId;
    private String algorithm;
    private List<String> attributeScopes;
    private String publicKeyFingerprint;
    private boolean active;

    public String getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(String authorityId) {
        this.authorityId = authorityId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public List<String> getAttributeScopes() {
        return attributeScopes;
    }

    public void setAttributeScopes(List<String> attributeScopes) {
        this.attributeScopes = attributeScopes;
    }

    public String getPublicKeyFingerprint() {
        return publicKeyFingerprint;
    }

    public void setPublicKeyFingerprint(String publicKeyFingerprint) {
        this.publicKeyFingerprint = publicKeyFingerprint;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
