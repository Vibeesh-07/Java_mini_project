package com.refind.service;

import com.refind.dto.claim.ClaimResponse;
import com.refind.dto.claim.CreateClaimRequest;
import com.refind.dto.claim.UpdateClaimStatusRequest;
import com.refind.entity.*;
import com.refind.exception.BadRequestException;
import com.refind.exception.ForbiddenException;
import com.refind.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ItemReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClaimStatusHistoryRepository claimStatusHistoryRepository;

    @Mock
    private ReportStatusHistoryRepository reportStatusHistoryRepository;

    private ClaimService claimService;

    private User finder;
    private User claimant;
    private ItemReport foundReport;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(
                claimRepository, reportRepository, userRepository,
                claimStatusHistoryRepository, reportStatusHistoryRepository
        );

        finder = new User("Finder", "finder@campus.edu", "hash", Role.ROLE_USER);
        finder.setId(1L);

        claimant = new User("Claimant", "claimant@campus.edu", "hash", Role.ROLE_USER);
        claimant.setId(2L);

        foundReport = new ItemReport();
        foundReport.setId(50L);
        foundReport.setReportType(ReportType.FOUND);
        foundReport.setStatus(ReportStatus.OPEN);
        foundReport.setTitle("Black Leather Wallet");
        foundReport.setDescription("Found near cafeteria");
        foundReport.setUser(finder);
    }

    @Test
    @DisplayName("Submit claim on open FOUND report transitions report to CLAIM_PENDING")
    void testSubmitClaimSuccess() {
        when(reportRepository.findById(50L)).thenReturn(Optional.of(foundReport));
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));
        when(claimRepository.existsByReportIdAndClaimantIdAndStatus(50L, 2L, ClaimStatus.PENDING)).thenReturn(false);

        Claim savedClaim = new Claim();
        savedClaim.setId(500L);
        savedClaim.setReport(foundReport);
        savedClaim.setClaimant(claimant);
        savedClaim.setClaimReason("Lost in cafeteria during lunch");
        savedClaim.setProofDetails("Student ID card with ending digits 4821 inside");
        savedClaim.setStatus(ClaimStatus.PENDING);

        when(claimRepository.save(any(Claim.class))).thenReturn(savedClaim);

        CreateClaimRequest req = new CreateClaimRequest("Lost in cafeteria during lunch", "Student ID card with ending digits 4821 inside");
        ClaimResponse res = claimService.submitClaim(50L, req, 2L);

        assertNotNull(res);
        assertEquals(500L, res.getId());
        assertEquals(ClaimStatus.PENDING, res.getStatus());
        assertEquals(ReportStatus.CLAIM_PENDING, foundReport.getStatus());

        verify(claimStatusHistoryRepository).save(any(ClaimStatusHistory.class));
        verify(reportStatusHistoryRepository).save(any(ReportStatusHistory.class));
    }

    @Test
    @DisplayName("Cannot submit claim on LOST report")
    void testCannotClaimLostReport() {
        foundReport.setReportType(ReportType.LOST);
        when(reportRepository.findById(50L)).thenReturn(Optional.of(foundReport));
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));

        CreateClaimRequest req = new CreateClaimRequest("Mine", "Proof");
        assertThrows(BadRequestException.class, () -> claimService.submitClaim(50L, req, 2L));
    }

    @Test
    @DisplayName("Finder cannot claim their own found item")
    void testFinderCannotClaimOwnItem() {
        when(reportRepository.findById(50L)).thenReturn(Optional.of(foundReport));
        when(userRepository.findById(finder.getId())).thenReturn(Optional.of(finder));

        CreateClaimRequest req = new CreateClaimRequest("Mine", "Proof");
        assertThrows(BadRequestException.class, () -> claimService.submitClaim(50L, req, finder.getId()));
    }

    @Test
    @DisplayName("Duplicate pending claim by same claimant is rejected")
    void testDuplicatePendingClaimRejected() {
        when(reportRepository.findById(50L)).thenReturn(Optional.of(foundReport));
        when(userRepository.findById(claimant.getId())).thenReturn(Optional.of(claimant));
        when(claimRepository.existsByReportIdAndClaimantIdAndStatus(50L, claimant.getId(), ClaimStatus.PENDING)).thenReturn(true);

        CreateClaimRequest req = new CreateClaimRequest("Mine again", "Proof again");
        assertThrows(BadRequestException.class, () -> claimService.submitClaim(50L, req, claimant.getId()));
    }

    @Test
    @DisplayName("Claimant cannot review or approve their own claim")
    void testClaimantCannotApproveOwnClaim() {
        Claim claim = new Claim();
        claim.setId(500L);
        claim.setReport(foundReport);
        claim.setClaimant(claimant);
        claim.setStatus(ClaimStatus.PENDING);

        when(claimRepository.findById(500L)).thenReturn(Optional.of(claim));
        when(userRepository.findById(claimant.getId())).thenReturn(Optional.of(claimant));

        UpdateClaimStatusRequest req = new UpdateClaimStatusRequest(ClaimStatus.APPROVED, "Approved by me");
        assertThrows(ForbiddenException.class, () -> claimService.reviewClaim(500L, req, claimant.getId(), false));
    }

    @Test
    @DisplayName("Approving claim marks report as RESOLVED and auto-rejects other pending claims")
    void testApproveClaimResolvesReportAndRejectsOthers() {
        Claim claim1 = new Claim();
        claim1.setId(501L);
        claim1.setReport(foundReport);
        claim1.setClaimant(claimant);
        claim1.setStatus(ClaimStatus.PENDING);

        User competitor = new User("Competitor", "comp@campus.edu", "hash", Role.ROLE_USER);
        competitor.setId(3L);

        Claim claim2 = new Claim();
        claim2.setId(502L);
        claim2.setReport(foundReport);
        claim2.setClaimant(competitor);
        claim2.setStatus(ClaimStatus.PENDING);

        when(claimRepository.findById(501L)).thenReturn(Optional.of(claim1));
        when(userRepository.findById(finder.getId())).thenReturn(Optional.of(finder));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(claimRepository.findByReportIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(claim1, claim2));

        UpdateClaimStatusRequest req = new UpdateClaimStatusRequest(ClaimStatus.APPROVED, "Verified student ID card");
        ClaimResponse res = claimService.reviewClaim(501L, req, finder.getId(), false);

        assertNotNull(res);
        assertEquals(ClaimStatus.APPROVED, res.getStatus());
        assertEquals(ReportStatus.RESOLVED, foundReport.getStatus());
        assertEquals(ClaimStatus.REJECTED, claim2.getStatus());
        verify(reportRepository).save(foundReport);
    }
}
