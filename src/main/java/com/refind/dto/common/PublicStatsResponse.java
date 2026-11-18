package com.refind.dto.common;

public class PublicStatsResponse {

    private long totalLostOpen;
    private long totalFoundOpen;
    private long totalResolved;
    private long totalPendingClaims;

    public PublicStatsResponse() {
    }

    public PublicStatsResponse(long totalLostOpen, long totalFoundOpen, long totalResolved, long totalPendingClaims) {
        this.totalLostOpen = totalLostOpen;
        this.totalFoundOpen = totalFoundOpen;
        this.totalResolved = totalResolved;
        this.totalPendingClaims = totalPendingClaims;
    }

    public long getTotalLostOpen() {
        return totalLostOpen;
    }

    public void setTotalLostOpen(long totalLostOpen) {
        this.totalLostOpen = totalLostOpen;
    }

    public long getTotalFoundOpen() {
        return totalFoundOpen;
    }

    public void setTotalFoundOpen(long totalFoundOpen) {
        this.totalFoundOpen = totalFoundOpen;
    }

    public long getTotalResolved() {
        return totalResolved;
    }

    public void setTotalResolved(long totalResolved) {
        this.totalResolved = totalResolved;
    }

    public long getTotalPendingClaims() {
        return totalPendingClaims;
    }

    public void setTotalPendingClaims(long totalPendingClaims) {
        this.totalPendingClaims = totalPendingClaims;
    }
}
