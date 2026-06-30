package com.qar.securitysystem.dto;

public class LabeOverviewResponse {
    private long totalFiles;
    private long latticeFiles;
    private long prototypeFiles;
    private long legacyFiles;
    private long totalPersons;
    private long totalUsers;
    private long userSecretBundles;
    private int authorityCount;

    public long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getLatticeFiles() {
        return latticeFiles;
    }

    public void setLatticeFiles(long latticeFiles) {
        this.latticeFiles = latticeFiles;
    }

    public long getPrototypeFiles() {
        return prototypeFiles;
    }

    public void setPrototypeFiles(long prototypeFiles) {
        this.prototypeFiles = prototypeFiles;
    }

    public long getLegacyFiles() {
        return legacyFiles;
    }

    public void setLegacyFiles(long legacyFiles) {
        this.legacyFiles = legacyFiles;
    }

    public long getTotalPersons() {
        return totalPersons;
    }

    public void setTotalPersons(long totalPersons) {
        this.totalPersons = totalPersons;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getUserSecretBundles() {
        return userSecretBundles;
    }

    public void setUserSecretBundles(long userSecretBundles) {
        this.userSecretBundles = userSecretBundles;
    }

    public int getAuthorityCount() {
        return authorityCount;
    }

    public void setAuthorityCount(int authorityCount) {
        this.authorityCount = authorityCount;
    }
}
