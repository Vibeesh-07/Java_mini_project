package com.refind.controller;

import com.refind.dto.claim.ClaimResponse;
import com.refind.dto.claim.CreateClaimRequest;
import com.refind.dto.claim.UpdateClaimStatusRequest;
import com.refind.exception.UnauthorizedException;
import com.refind.security.SecurityUtils;
import com.refind.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/api/reports/{id}/claims")
    public ResponseEntity<ClaimResponse> submitClaim(
            @PathVariable Long id,
            @Valid @RequestBody CreateClaimRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to claim an item"));
        ClaimResponse response = claimService.submitClaim(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/reports/{id}/claims")
    public ResponseEntity<List<ClaimResponse>> getClaimsForReport(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to view claims"));
        boolean isAdmin = SecurityUtils.isAdmin();
        return ResponseEntity.ok(claimService.getClaimsForReport(id, currentUserId, isAdmin));
    }

    @GetMapping("/api/users/me/claims")
    public ResponseEntity<List<ClaimResponse>> getMySubmittedClaims() {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to view your claims"));
        return ResponseEntity.ok(claimService.getClaimsSubmittedByUser(currentUserId));
    }

    @GetMapping("/api/users/me/received-claims")
    public ResponseEntity<List<ClaimResponse>> getMyReceivedClaims() {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to view received claims"));
        return ResponseEntity.ok(claimService.getClaimsReceivedByUser(currentUserId));
    }

    @PatchMapping("/api/claims/{id}/status")
    public ResponseEntity<ClaimResponse> reviewClaim(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClaimStatusRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to review claims"));
        boolean isAdmin = SecurityUtils.isAdmin();
        ClaimResponse response = claimService.reviewClaim(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }
}
