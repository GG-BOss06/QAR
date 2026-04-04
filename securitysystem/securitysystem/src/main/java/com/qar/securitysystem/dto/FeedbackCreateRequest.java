package com.qar.securitysystem.dto;

public class FeedbackCreateRequest {
    private String type;
    private String subject;
    private String message;
    private String contact;
    private String relatedFileId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getRelatedFileId() {
        return relatedFileId;
    }

    public void setRelatedFileId(String relatedFileId) {
        this.relatedFileId = relatedFileId;
    }
}

