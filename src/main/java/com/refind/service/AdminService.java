package com.refind.service;

import com.refind.dto.admin.ModerateReportRequest;
import com.refind.dto.claim.ClaimResponse;
import com.refind.dto.common.PagedResponse;
import com.refind.dto.report.ReportResponse;
import com.refind.entity.*;
import com.refind.exception.ResourceNotFoundException;
import com.refind.repository.ClaimRepository;
import com.refind.repository.ItemReportRepository;
import com.refind.repository.ReportStatusHistoryRepository;
import com.refind.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final ItemReportRepository reportRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ReportStatusHistoryRepository reportStatusHistoryRepository;

    public AdminService(
            ItemReportRepository reportRepository,
            ClaimRepository claimRepository,
            UserRepository userRepository,
            ReportStatusHistoryRepository reportStatusHistoryRepository
    ) {
        this.reportRepository = reportRepository;
        this.claimRepository = claimRepository;
        this.userRepository = userRepository;
        this.reportStatusHistoryRepository = reportStatusHistoryRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReportResponse> getAllReports(Pageable pageable, Long adminId) {
        int pageSize = Math.min(pageable.getPageSize(), 50);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by("createdAt").descending());
        Page<ItemReport> page = reportRepository.findAll(safePageable);
        List<ReportResponse> mapped = page.getContent().stream()
                .map(r -> ReportResponse.fromEntity(r, adminId))
                .toList();
        return PagedResponse.of(page, mapped);
    }

    @Transactional
    public ReportResponse moderateReport(Long reportId, ModerateReportRequest request, Long adminId) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        ReportStatus oldStatus = report.getStatus();
        ReportStatus newStatus = request.getStatus();

        report.setStatus(newStatus);
        ItemReport saved = reportRepository.save(report);

        String note = request.getModerationNote() != null && !request.getModerationNote().isBlank()
                ? "Admin moderation: " + request.getModerationNote().trim()
                : "Admin moderation status changed to " + newStatus;

        ReportStatusHistory rsh = new ReportStatusHistory(saved, oldStatus, newStatus, admin, note);
        reportStatusHistoryRepository.save(rsh);

        return ReportResponse.fromEntity(saved, adminId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClaimResponse> getAllClaims(Pageable pageable, Long adminId) {
        int pageSize = Math.min(pageable.getPageSize(), 50);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by("createdAt").descending());
        Page<Claim> page = claimRepository.findAllByOrderByCreatedAtDesc(safePageable);
        List<ClaimResponse> mapped = page.getContent().stream()
                .map(c -> ClaimResponse.fromEntity(c, adminId, true))
                .toList();
        return PagedResponse.of(page, mapped);
    }
}
