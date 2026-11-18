package com.refind.dto.admin;

public class AdminDashboardResponse {

    private long totalUsers;
    private long totalReports;
    private long openLostReports;
    private long openFoundReports;
    private long resolvedReports;
    private long cancelledReports;
    private long pendingClaims;
    private long approvedClaims;
    private long rejectedClaims;

    public AdminDashboardResponse() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getOpenLostReports() {
        return openLostReports;
    }

    public void setOpenLostReports(long openLostReports) {
        this.openLostReports = openLostReports;
    }

    public long getOpenFoundReports() {
        return openFoundReports;
    }

    public void setOpenFoundReports(long openFoundReports) {
        this.openFoundReports = openFoundReports;
    }

    public long getResolvedReports() {
        return resolvedReports;
    }

    public void setResolvedReports(long resolvedReports) {
        this.resolvedReports = resolvedReports;
    }

    public long getCancelledReports() {
        return cancelledReports;
    }

    public void setCancelledReports(long cancelledReports) {
        this.cancelledReports = cancelledReports;
    }

    public long getPendingClaims() {
        return pendingClaims;
    }

    public void setPendingClaims(long pendingClaims) {
        this.pendingClaims = pendingClaims;
    }

    public long getApprovedClaims() {
        return approvedClaims;
    }

    public void setApprovedClaims(long approvedClaims) {
        this.approvedClaims = approvedClaims;
    }

    public long getRejectedClaims() {
        return rejectedClaims;
    }

    public void setRejectedClaims(long rejectedClaims) {
        this.rejectedClaims = rejectedClaims;
    }
}
