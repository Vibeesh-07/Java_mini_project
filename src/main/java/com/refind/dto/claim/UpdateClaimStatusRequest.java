package com.refind.dto.claim;

import com.refind.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateClaimStatusRequest {

    @NotNull(message = "Claim status is required")
    private ClaimStatus status;

    @Size(max = 255, message = "Review notes must not exceed 255 characters")
    private String reviewNotes;

    public UpdateClaimStatusRequest() {
    }

    public UpdateClaimStatusRequest(ClaimStatus status, String reviewNotes) {
        this.status = status;
        this.reviewNotes = reviewNotes;
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
}
