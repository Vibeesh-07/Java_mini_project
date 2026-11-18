package com.refind.service;

import com.refind.dto.admin.AdminDashboardResponse;
import com.refind.dto.common.PublicStatsResponse;
import com.refind.entity.ClaimStatus;
import com.refind.entity.ReportStatus;
import com.refind.entity.ReportType;
import com.refind.repository.ClaimRepository;
import com.refind.repository.ItemReportRepository;
import com.refind.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {

    private final ItemReportRepository reportRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    public StatisticsService(
            ItemReportRepository reportRepository,
            ClaimRepository claimRepository,
            UserRepository userRepository
    ) {
        this.reportRepository = reportRepository;
        this.claimRepository = claimRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PublicStatsResponse getPublicStats() {
        long lostOpen = reportRepository.countByReportTypeAndStatus(ReportType.LOST, ReportStatus.OPEN);
        long foundOpen = reportRepository.countByReportTypeAndStatus(ReportType.FOUND, ReportStatus.OPEN);
        long resolved = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long pendingClaims = claimRepository.countByStatus(ClaimStatus.PENDING);

        return new PublicStatsResponse(lostOpen, foundOpen, resolved, pendingClaims);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboardStats() {
        AdminDashboardResponse stats = new AdminDashboardResponse();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalReports(reportRepository.count());
        stats.setOpenLostReports(reportRepository.countByReportTypeAndStatus(ReportType.LOST, ReportStatus.OPEN));
        stats.setOpenFoundReports(reportRepository.countByReportTypeAndStatus(ReportType.FOUND, ReportStatus.OPEN));
        stats.setResolvedReports(reportRepository.countByStatus(ReportStatus.RESOLVED));
        stats.setCancelledReports(reportRepository.countByStatus(ReportStatus.CANCELLED));
        stats.setPendingClaims(claimRepository.countByStatus(ClaimStatus.PENDING));
        stats.setApprovedClaims(claimRepository.countByStatus(ClaimStatus.APPROVED));
        stats.setRejectedClaims(claimRepository.countByStatus(ClaimStatus.REJECTED));
        return stats;
    }
}
