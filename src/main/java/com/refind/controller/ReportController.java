package com.refind.controller;

import com.refind.dto.common.PagedResponse;
import com.refind.dto.matching.MatchResultResponse;
import com.refind.dto.report.CreateReportRequest;
import com.refind.dto.report.ReportResponse;
import com.refind.dto.report.UpdateReportRequest;
import com.refind.dto.report.UpdateReportStatusRequest;
import com.refind.entity.ItemReport;
import com.refind.entity.ReportStatus;
import com.refind.entity.ReportType;
import com.refind.exception.UnauthorizedException;
import com.refind.matching.MatchingService;
import com.refind.security.SecurityUtils;
import com.refind.service.ItemReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
public class ReportController {

    private final ItemReportService reportService;
    private final MatchingService matchingService;

    public ReportController(ItemReportService reportService, MatchingService matchingService) {
        this.reportService = reportService;
        this.matchingService = matchingService;
    }

    @GetMapping("/api/reports")
    public ResponseEntity<PagedResponse<ReportResponse>> searchReports(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String locationZone,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Set<String> permittedSortFields = Set.of("createdAt", "incidentDate", "title");
        String safeSortBy = permittedSortFields.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(safeSortBy).ascending() : Sort.by(safeSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), sort);

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        PagedResponse<ReportResponse> response = reportService.searchReports(
                query, reportType, categoryId, locationZone, status, pageable, currentUserId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/reports/recent")
    public ResponseEntity<List<ReportResponse>> getRecentReports(
            @RequestParam(defaultValue = "6") int limit
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(reportService.getRecentReports(limit, currentUserId));
    }

    @GetMapping("/api/reports/{id}")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return ResponseEntity.ok(reportService.getReportById(id, currentUserId));
    }

    @GetMapping("/api/reports/{id}/image")
    public ResponseEntity<byte[]> getReportImage(@PathVariable Long id) {
        return reportService.getReportImage(id);
    }

    @PostMapping("/api/reports")
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to report an item"));
        ReportResponse response = reportService.createReport(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/reports/{id}")
    public ResponseEntity<ReportResponse> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to update a report"));
        boolean isAdmin = SecurityUtils.isAdmin();
        ReportResponse response = reportService.updateReport(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/reports/{id}/status")
    public ResponseEntity<ReportResponse> updateReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to change report status"));
        boolean isAdmin = SecurityUtils.isAdmin();
        ReportResponse response = reportService.updateReportStatus(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/reports/{id}/custody")
    public ResponseEntity<ReportResponse> updateCustody(
            @PathVariable Long id,
            @Valid @RequestBody com.refind.dto.report.UpdateCustodyRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to update item custody"));
        return ResponseEntity.ok(reportService.updateCustody(id, request, currentUserId, SecurityUtils.isAdmin()));
    }

    @GetMapping("/api/reports/{id}/matches")
    public ResponseEntity<List<MatchResultResponse>> getReportMatches(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ItemReport report = reportService.getReportEntityById(id);
        List<MatchResultResponse> matches = matchingService.findMatches(report, currentUserId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/api/users/me/reports")
    public ResponseEntity<PagedResponse<ReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("You must be logged in to view your reports"));
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(reportService.getUserReports(currentUserId, pageable));
    }
}
