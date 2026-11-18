package com.refind.service;

import com.refind.dto.claim.ClaimResponse;
import com.refind.dto.claim.CreateClaimRequest;
import com.refind.dto.claim.UpdateClaimStatusRequest;
import com.refind.entity.*;
import com.refind.exception.BadRequestException;
import com.refind.exception.ForbiddenException;
import com.refind.exception.ResourceNotFoundException;
import com.refind.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final ReportStatusHistoryRepository reportStatusHistoryRepository;

    public ClaimService(
            ClaimRepository claimRepository,
            ItemReportRepository reportRepository,
            UserRepository userRepository,
            ClaimStatusHistoryRepository claimStatusHistoryRepository,
            ReportStatusHistoryRepository reportStatusHistoryRepository
    ) {
        this.claimRepository = claimRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.claimStatusHistoryRepository = claimStatusHistoryRepository;
        this.reportStatusHistoryRepository = reportStatusHistoryRepository;
    }

    @Transactional
    public ClaimResponse submitClaim(Long reportId, CreateClaimRequest request, Long claimantId) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        User claimant = userRepository.findById(claimantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + claimantId));

        // 1. Can only claim FOUND items
        if (report.getReportType() != ReportType.FOUND) {
            throw new BadRequestException("Claims can only be filed against FOUND items");
        }

        // 2. Cannot claim already resolved or cancelled items
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.CANCELLED) {
            throw new BadRequestException("Cannot claim an item that is already resolved or cancelled");
        }

        // 3. Finder cannot claim their own found item
        if (report.getUser().getId().equals(claimantId)) {
            throw new BadRequestException("You cannot submit a claim against an item you reported as found");
        }

        // 4. Prevent duplicate pending claims by the same claimant
        if (claimRepository.existsByReportIdAndClaimantIdAndStatus(reportId, claimantId, ClaimStatus.PENDING)) {
            throw new BadRequestException("You already have a pending claim submitted for this item");
        }

        Claim claim = new Claim();
        claim.setReport(report);
        claim.setClaimant(claimant);
        claim.setClaimReason(request.getClaimReason().trim());
        claim.setProofDetails(request.getProofDetails().trim());
        claim.setStatus(ClaimStatus.PENDING);

        Claim savedClaim = claimRepository.save(claim);

        // Audit log claim creation
        ClaimStatusHistory csh = new ClaimStatusHistory(
                savedClaim,
                ClaimStatus.PENDING,
                ClaimStatus.PENDING,
                claimant,
                "Claim submitted"
        );
        claimStatusHistoryRepository.save(csh);

        // Update report status to CLAIM_PENDING if currently OPEN
        if (report.getStatus() == ReportStatus.OPEN) {
            ReportStatus oldStatus = report.getStatus();
            report.setStatus(ReportStatus.CLAIM_PENDING);
            reportRepository.save(report);

            ReportStatusHistory rsh = new ReportStatusHistory(
                    report,
                    oldStatus,
                    ReportStatus.CLAIM_PENDING,
                    claimant,
                    "Claim filed; report moved to CLAIM_PENDING"
            );
            reportStatusHistoryRepository.save(rsh);
        }

        return ClaimResponse.fromEntity(savedClaim, claimantId, false);
    }

    @Transactional
    public ClaimResponse reviewClaim(Long claimId, UpdateClaimStatusRequest request, Long reviewerId, boolean isAdmin) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reviewerId));

        ItemReport report = claim.getReport();

        // Prevent users from reviewing/approving their own claims!
        if (claim.getClaimant().getId().equals(reviewerId)) {
            throw new ForbiddenException("You cannot approve or reject your own claim");
        }

        // Must be the finder who posted the report, or an administrator
        boolean isReportOwner = report.getUser().getId().equals(reviewerId);
        if (!isReportOwner && !isAdmin) {
            throw new ForbiddenException("Only the finder or an administrator can review this claim");
        }

        // Must be currently PENDING to review
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new BadRequestException("Claim is already " + claim.getStatus() + " and cannot be reviewed again");
        }

        ClaimStatus newStatus = request.getStatus();
        if (newStatus != ClaimStatus.APPROVED && newStatus != ClaimStatus.REJECTED) {
            throw new BadRequestException("Review status must be either APPROVED or REJECTED");
        }

        ClaimStatus oldStatus = claim.getStatus();
        claim.setStatus(newStatus);
        claim.setReviewNotes(request.getReviewNotes() != null ? request.getReviewNotes().trim() : null);
        claim.setReviewedAt(LocalDateTime.now());
        Claim savedClaim = claimRepository.save(claim);

        // Log claim status history
        ClaimStatusHistory csh = new ClaimStatusHistory(
                savedClaim,
                oldStatus,
                newStatus,
                reviewer,
                "Claim reviewed by " + (isAdmin ? "admin" : "finder")
        );
        claimStatusHistoryRepository.save(csh);

        if (newStatus == ClaimStatus.APPROVED) {
            // Resolve the report
            ReportStatus oldReportStatus = report.getStatus();
            report.setStatus(ReportStatus.RESOLVED);
            reportRepository.save(report);

            ReportStatusHistory rsh = new ReportStatusHistory(
                    report,
                    oldReportStatus,
                    ReportStatus.RESOLVED,
                    reviewer,
                    "Claim #" + claim.getId() + " approved. Item marked as RESOLVED"
            );
            reportStatusHistoryRepository.save(rsh);

            // Automatically reject all other pending claims on this report
            List<Claim> otherClaims = claimRepository.findByReportIdOrderByCreatedAtDesc(report.getId());
            for (Claim other : otherClaims) {
                if (!other.getId().equals(claim.getId()) && other.getStatus() == ClaimStatus.PENDING) {
                    other.setStatus(ClaimStatus.REJECTED);
                    other.setReviewNotes("Another claim was verified and approved for this item");
                    other.setReviewedAt(LocalDateTime.now());
                    claimRepository.save(other);

                    ClaimStatusHistory otherCsh = new ClaimStatusHistory(
                            other,
                            ClaimStatus.PENDING,
                            ClaimStatus.REJECTED,
                            reviewer,
                            "Auto-rejected because item was claimed by another user"
                    );
                    claimStatusHistoryRepository.save(otherCsh);
                }
            }
        } else if (newStatus == ClaimStatus.REJECTED) {
            // If no other pending claims exist for this report, revert report to OPEN
            List<Claim> allClaims = claimRepository.findByReportIdOrderByCreatedAtDesc(report.getId());
            boolean hasOtherPending = allClaims.stream()
                    .anyMatch(c -> !c.getId().equals(claim.getId()) && c.getStatus() == ClaimStatus.PENDING);

            if (!hasOtherPending && report.getStatus() == ReportStatus.CLAIM_PENDING) {
                ReportStatus oldReportStatus = report.getStatus();
                report.setStatus(ReportStatus.OPEN);
                reportRepository.save(report);

                ReportStatusHistory rsh = new ReportStatusHistory(
                        report,
                        oldReportStatus,
                        ReportStatus.OPEN,
                        reviewer,
                        "Claim #" + claim.getId() + " rejected; no other pending claims, reverted to OPEN"
                );
                reportStatusHistoryRepository.save(rsh);
            }
        }

        return ClaimResponse.fromEntity(savedClaim, reviewerId, isAdmin);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getClaimsForReport(Long reportId, Long currentUserId, boolean isAdmin) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        boolean isReportOwner = report.getUser().getId().equals(currentUserId);
        if (!isReportOwner && !isAdmin) {
            throw new ForbiddenException("Only the item reporter or an admin can view claims on this report");
        }

        return claimRepository.findByReportIdOrderByCreatedAtDesc(reportId).stream()
                .map(c -> ClaimResponse.fromEntity(c, currentUserId, isAdmin))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getClaimsSubmittedByUser(Long claimantId) {
        return claimRepository.findByClaimantIdOrderByCreatedAtDesc(claimantId).stream()
                .map(c -> ClaimResponse.fromEntity(c, claimantId, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getClaimsReceivedByUser(Long ownerId) {
        return claimRepository.findByReportUserIdOrderByCreatedAtDesc(ownerId).stream()
                .map(c -> ClaimResponse.fromEntity(c, ownerId, false))
                .toList();
    }
}
