package com.refind.service;

import com.refind.dto.common.PagedResponse;
import com.refind.dto.report.*;
import com.refind.entity.*;
import com.refind.exception.BadRequestException;
import com.refind.exception.ForbiddenException;
import com.refind.exception.ResourceNotFoundException;
import com.refind.repository.CategoryRepository;
import com.refind.repository.ItemReportRepository;
import com.refind.repository.ReportStatusHistoryRepository;
import com.refind.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class ItemReportService {

    private final ItemReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReportStatusHistoryRepository statusHistoryRepository;

    public ItemReportService(
            ItemReportRepository reportRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ReportStatusHistoryRepository statusHistoryRepository
    ) {
        this.reportRepository = reportRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public ReportResponse createReport(CreateReportRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        ItemReport report = new ItemReport();
        report.setReportType(request.getReportType());
        report.setTitle(request.getTitle().trim());
        report.setDescription(request.getDescription().trim());
        report.setCategory(category);
        report.setColor(request.getColor() != null ? request.getColor().trim() : null);
        report.setBrand(request.getBrand() != null ? request.getBrand().trim() : null);
        report.setLocationName(request.getLocationName().trim());
        report.setLocationZone(request.getLocationZone() != null && !request.getLocationZone().isBlank()
                ? request.getLocationZone().trim()
                : "CAMPUS_GENERAL");
        report.setIncidentDate(request.getIncidentDate());
        report.setStatus(ReportStatus.OPEN);
        report.setUser(user);

        // Optional photo attachment
        if (request.getImageData() != null && !request.getImageData().isBlank()) {
            report.setImageData(validateAndCleanImageData(request.getImageData()));
        }

        ItemReport saved = reportRepository.save(report);

        // Audit log creation
        ReportStatusHistory history = new ReportStatusHistory(
                saved,
                ReportStatus.OPEN,
                ReportStatus.OPEN,
                user,
                "Report initial creation"
        );
        statusHistoryRepository.save(history);

        return ReportResponse.fromEntity(saved, userId);
    }

    @Transactional
    public ReportResponse updateReport(Long reportId, UpdateReportRequest request, Long userId, boolean isAdmin) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        if (!isAdmin && !report.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only modify your own reports");
        }

        if (report.getStatus() != ReportStatus.OPEN) {
            throw new BadRequestException("Only reports in OPEN status can be edited");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        report.setTitle(request.getTitle().trim());
        report.setDescription(request.getDescription().trim());
        report.setCategory(category);
        report.setColor(request.getColor() != null ? request.getColor().trim() : null);
        report.setBrand(request.getBrand() != null ? request.getBrand().trim() : null);
        report.setLocationName(request.getLocationName().trim());
        report.setLocationZone(request.getLocationZone() != null && !request.getLocationZone().isBlank()
                ? request.getLocationZone().trim()
                : "CAMPUS_GENERAL");
        report.setIncidentDate(request.getIncidentDate());

        // Optional photo update or removal
        if (Boolean.TRUE.equals(request.getRemoveImage())) {
            report.setImageData(null);
        } else if (request.getImageData() != null && !request.getImageData().isBlank()) {
            report.setImageData(validateAndCleanImageData(request.getImageData()));
        }

        ItemReport saved = reportRepository.save(report);
        return ReportResponse.fromEntity(saved, userId);
    }

    @Transactional
    public ReportResponse updateReportStatus(Long reportId, UpdateReportStatusRequest request, Long userId, boolean isAdmin) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!isAdmin && !report.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only update the status of your own reports");
        }

        ReportStatus oldStatus = report.getStatus();
        ReportStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return ReportResponse.fromEntity(report, userId);
        }

        // Validate allowed transitions
        if (!isAdmin) {
            if (oldStatus == ReportStatus.RESOLVED || oldStatus == ReportStatus.CANCELLED) {
                throw new BadRequestException("Closed reports cannot be reopened");
            }
            if (newStatus != ReportStatus.RESOLVED && newStatus != ReportStatus.CANCELLED) {
                throw new BadRequestException("Permitted status updates for report owners are RESOLVED or CANCELLED");
            }
        }

        report.setStatus(newStatus);
        ItemReport saved = reportRepository.save(report);

        // Audit log status transition
        ReportStatusHistory history = new ReportStatusHistory(
                saved,
                oldStatus,
                newStatus,
                currentUser,
                request.getComments() != null ? request.getComments().trim() : "Status updated by " + (isAdmin ? "admin" : "owner")
        );
        statusHistoryRepository.save(history);

        return ReportResponse.fromEntity(saved, userId);
    }

    @Transactional
    public ReportResponse updateCustody(Long reportId, UpdateCustodyRequest request, Long userId, boolean isAdmin) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
        if (!isAdmin && !report.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only update custody for your own reports");
        }
        if (report.getReportType() != ReportType.FOUND) {
            throw new BadRequestException("Only FOUND reports can be handed to a campus office");
        }
        if (report.getStatus() != ReportStatus.OPEN && report.getStatus() != ReportStatus.CLAIM_PENDING) {
            throw new BadRequestException("Custody cannot be changed on a closed report");
        }

        if (request.isHandedToOffice()) {
            if (isBlank(request.getOfficeName()) || isBlank(request.getOfficeContact())) {
                throw new BadRequestException("Office name and office contact are required when an item is handed over");
            }
            report.setHandedToOffice(true);
            report.setOfficeName(request.getOfficeName().trim());
            report.setOfficeContact(request.getOfficeContact().trim());
            report.setOfficeNotes(normalizeOptional(request.getOfficeNotes()));
            report.setHandedOverAt(LocalDateTime.now());
        } else {
            report.setHandedToOffice(false);
            report.setOfficeName(null);
            report.setOfficeContact(null);
            report.setOfficeNotes(null);
            report.setHandedOverAt(null);
        }
        return ReportResponse.fromEntity(reportRepository.save(report), userId);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId, Long currentUserId) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
        return ReportResponse.fromEntity(report, currentUserId);
    }

    @Transactional(readOnly = true)
    public ItemReport getReportEntityById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReportResponse> searchReports(
            String query,
            ReportType reportType,
            Long categoryId,
            String locationZone,
            ReportStatus status,
            Pageable pageable,
            Long currentUserId
    ) {
        // Enforce maximum page size limit
        int pageSize = Math.min(pageable.getPageSize(), 50);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());

        Specification<ItemReport> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (reportType != null) {
                predicates.add(cb.equal(root.get("reportType"), reportType));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (locationZone != null && !locationZone.isBlank() && !locationZone.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(root.get("locationZone"), locationZone.trim()));
            }

            // Browse endpoints are public; never expose closed reports through URL parameters.
            if (status == ReportStatus.OPEN || status == ReportStatus.CLAIM_PENDING) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(root.get("status").in(ReportStatus.OPEN, ReportStatus.CLAIM_PENDING));
            }

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate colorLike = cb.like(cb.lower(root.get("color")), pattern);
                Predicate brandLike = cb.like(cb.lower(root.get("brand")), pattern);
                Predicate locLike = cb.like(cb.lower(root.get("locationName")), pattern);
                predicates.add(cb.or(titleLike, descLike, colorLike, brandLike, locLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ItemReport> page = reportRepository.findAll(spec, safePageable);
        List<ReportResponse> mapped = page.getContent().stream()
                .map(r -> ReportResponse.fromEntity(r, currentUserId))
                .toList();

        return PagedResponse.of(page, mapped);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReportResponse> getUserReports(Long userId, Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), 50);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by("createdAt").descending());
        Page<ItemReport> page = reportRepository.findByUserIdOrderByCreatedAtDesc(userId, safePageable);
        List<ReportResponse> mapped = page.getContent().stream()
                .map(r -> ReportResponse.fromEntity(r, userId))
                .toList();
        return PagedResponse.of(page, mapped);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getRecentReports(int limit, Long currentUserId) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 12), Sort.by("createdAt").descending());
        Specification<ItemReport> spec = (root, query, cb) -> cb.or(
                cb.equal(root.get("status"), ReportStatus.OPEN),
                cb.equal(root.get("status"), ReportStatus.CLAIM_PENDING)
        );
        Page<ItemReport> page = reportRepository.findAll(spec, pageable);
        return page.getContent().stream()
                .map(r -> ReportResponse.fromEntity(r, currentUserId))
                .toList();
    }

    private static final int MAX_IMAGE_BASE64_LENGTH = 4 * 1024 * 1024; // ~3MB Data URL -> ~2.2MB raw image

    private String validateAndCleanImageData(String imageData) {
        if (imageData == null || imageData.isBlank()) {
            return null;
        }
        String trimmed = imageData.trim();
        if (trimmed.length() > MAX_IMAGE_BASE64_LENGTH) {
            throw new BadRequestException("Image file is too large. Maximum supported image size is 2MB.");
        }
        if (!trimmed.matches("^data:image/(jpeg|png|webp|gif);base64,[A-Za-z0-9+/=\\r\\n]+$")) {
            throw new BadRequestException("Invalid image format. Supported formats: JPEG, PNG, WebP, GIF.");
        }
        return trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getReportImage(Long reportId) {
        ItemReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        if (report.getImageData() == null || report.getImageData().isBlank()) {
            throw new ResourceNotFoundException("Report has no attached image");
        }

        String data = report.getImageData().trim();
        String mimeType = "image/jpeg";
        byte[] imageBytes;

        if (data.startsWith("data:") && data.contains(";base64,")) {
            int semiIdx = data.indexOf(";base64,");
            mimeType = data.substring(5, semiIdx);
            String base64Str = data.substring(semiIdx + 8);
            try {
                imageBytes = Base64.getDecoder().decode(base64Str);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Corrupt image data");
            }
        } else {
            try {
                imageBytes = Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Corrupt image data");
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(mimeType))
                .body(imageBytes);
    }
}
