package com.refind.dto.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateClaimRequest {

    @NotBlank(message = "Claim reason is required")
    @Size(min = 5, message = "Claim reason must be at least 5 characters")
    private String claimReason;

    @NotBlank(message = "Proof / identifying details are required")
    @Size(min = 5, message = "Proof details must be at least 5 characters")
    private String proofDetails;

    public CreateClaimRequest() {
    }

    public CreateClaimRequest(String claimReason, String proofDetails) {
        this.claimReason = claimReason;
        this.proofDetails = proofDetails;
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
}
