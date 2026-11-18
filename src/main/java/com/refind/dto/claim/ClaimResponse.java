package com.refind.dto.claim;

import com.refind.entity.Claim;
import com.refind.entity.ClaimStatus;
import com.refind.entity.ReportType;

import java.time.LocalDateTime;

public class ClaimResponse {

    private Long id;
    private Long reportId;
    private String reportTitle;
    private ReportType reportType;
    private Long claimantId;
    private String claimantName;
    private Long reportOwnerId;
    private String reportOwnerName;
    private String claimReason;
    private String proofDetails;
    private ClaimStatus status;
    private String reviewNotes;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canManage;

    public ClaimResponse() {
    }

    public static ClaimResponse fromEntity(Claim claim, Long currentUserId, boolean isAdmin) {
        if (claim == null) return null;
        ClaimResponse dto = new ClaimResponse();
        dto.setId(claim.getId());

        if (claim.getReport() != null) {
            dto.setReportId(claim.getReport().getId());
            dto.setReportTitle(claim.getReport().getTitle());
            dto.setReportType(claim.getReport().getReportType());
            if (claim.getReport().getUser() != null) {
                dto.setReportOwnerId(claim.getReport().getUser().getId());
                dto.setReportOwnerName(claim.getReport().getUser().getName());
            }
        }

        if (claim.getClaimant() != null) {
            dto.setClaimantId(claim.getClaimant().getId());
            dto.setClaimantName(claim.getClaimant().getName());
        }

        dto.setClaimReason(claim.getClaimReason());

        // Private proof details are only visible to the claimant, the report owner, or an administrator
        boolean isClaimant = currentUserId != null && claim.getClaimant() != null && currentUserId.equals(claim.getClaimant().getId());
        boolean isReportOwner = currentUserId != null && claim.getReport() != null && claim.getReport().getUser() != null && currentUserId.equals(claim.getReport().getUser().getId());

        if (isClaimant || isReportOwner || isAdmin) {
            dto.setProofDetails(claim.getProofDetails());
        } else {
            dto.setProofDetails("[Protected: Visible only to item finder and administrator]");
        }

        dto.setStatus(claim.getStatus());
        dto.setReviewNotes(claim.getReviewNotes());
        dto.setReviewedAt(claim.getReviewedAt());
        dto.setCreatedAt(claim.getCreatedAt());
        dto.setUpdatedAt(claim.getUpdatedAt());

        // Can manage if user is the finder/owner or an admin, and claim is currently PENDING
        dto.setCanManage((isReportOwner || isAdmin) && claim.getStatus() == ClaimStatus.PENDING);

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public Long getClaimantId() {
        return claimantId;
    }

    public void setClaimantId(Long claimantId) {
        this.claimantId = claimantId;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public Long getReportOwnerId() {
        return reportOwnerId;
    }

    public void setReportOwnerId(Long reportOwnerId) {
        this.reportOwnerId = reportOwnerId;
    }

    public String getReportOwnerName() {
        return reportOwnerName;
    }

    public void setReportOwnerName(String reportOwnerName) {
        this.reportOwnerName = reportOwnerName;
    }

    public String getClaimReason() {
        return claimReason;
    }

    public void setClaimReason(String claimReason) {
        this.claimReason = claimReason;
    }

    public String getProofDetails() {
        return proofDetails;
    }

    public void setProofDetails(String proofDetails) {
        this.proofDetails = proofDetails;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isCanManage() {
        return canManage;
    }

    public void setCanManage(boolean canManage) {
        this.canManage = canManage;
    }
}
