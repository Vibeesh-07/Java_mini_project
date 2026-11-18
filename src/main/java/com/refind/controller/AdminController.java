package com.refind.controller;

import com.refind.dto.admin.AdminDashboardResponse;
import com.refind.dto.admin.ModerateReportRequest;
import com.refind.dto.claim.ClaimResponse;
import com.refind.dto.common.PagedResponse;
import com.refind.dto.report.ReportResponse;
import com.refind.exception.UnauthorizedException;
import com.refind.security.SecurityUtils;
import com.refind.service.AdminService;
import com.refind.service.StatisticsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final StatisticsService statisticsService;

    public AdminController(AdminService adminService, StatisticsService statisticsService) {
        this.adminService = adminService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboardMetrics() {
        return ResponseEntity.ok(statisticsService.getAdminDashboardStats());
    }

    @GetMapping("/reports")
    public ResponseEntity<PagedResponse<ReportResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Long adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Admin authorization required"));
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(adminService.getAllReports(pageable, adminId));
    }

    @PatchMapping("/reports/{id}/moderation")
    public ResponseEntity<ReportResponse> moderateReport(
            @PathVariable Long id,
            @Valid @RequestBody ModerateReportRequest request
    ) {
        Long adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Admin authorization required"));
        return ResponseEntity.ok(adminService.moderateReport(id, request, adminId));
    }

    @GetMapping("/claims")
    public ResponseEntity<PagedResponse<ClaimResponse>> getClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Long adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Admin authorization required"));
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(adminService.getAllClaims(pageable, adminId));
    }
}
