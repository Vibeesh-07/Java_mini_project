package com.refind.service;

import com.refind.dto.report.CreateReportRequest;
import com.refind.dto.report.ReportResponse;
import com.refind.dto.report.UpdateReportRequest;
import com.refind.dto.report.UpdateReportStatusRequest;
import com.refind.entity.*;
import com.refind.exception.BadRequestException;
import com.refind.exception.ForbiddenException;
import com.refind.repository.CategoryRepository;
import com.refind.repository.ItemReportRepository;
import com.refind.repository.ReportStatusHistoryRepository;
import com.refind.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemReportServiceTest {

    @Mock
    private ItemReportRepository reportRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportStatusHistoryRepository statusHistoryRepository;

    private ItemReportService reportService;

    private User owner;
    private User otherUser;
    private Category category;

    @BeforeEach
    void setUp() {
        reportService = new ItemReportService(reportRepository, categoryRepository, userRepository, statusHistoryRepository);

        owner = new User("Owner", "owner@campus.edu", "hash", Role.ROLE_USER);
        owner.setId(1L);

        otherUser = new User("Other", "other@campus.edu", "hash", Role.ROLE_USER);
        otherUser.setId(2L);

        category = new Category("Electronics", "Gadgets", "laptop");
        category.setId(10L);
    }

    @Test
    @DisplayName("Create report persists entity and writes initial status history")
    void testCreateReportSuccess() {
        CreateReportRequest req = new CreateReportRequest();
        req.setReportType(ReportType.LOST);
        req.setTitle("Blue Backpack");
        req.setDescription("Left on bench in quad");
        req.setCategoryId(10L);
        req.setColor("Navy Blue");
        req.setBrand("Nike");
        req.setLocationName("Campus Quad Bench");
        req.setLocationZone("CAMPUS_GENERAL");
        req.setIncidentDate(LocalDate.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        ItemReport saved = new ItemReport();
        saved.setId(100L);
        saved.setReportType(ReportType.LOST);
        saved.setTitle("Blue Backpack");
        saved.setDescription("Left on bench in quad");
        saved.setCategory(category);
        saved.setUser(owner);
        saved.setStatus(ReportStatus.OPEN);
        saved.setLocationName("Campus Quad Bench");
        saved.setIncidentDate(LocalDate.now());

        when(reportRepository.save(any(ItemReport.class))).thenReturn(saved);

        ReportResponse res = reportService.createReport(req, 1L);

        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals("Blue Backpack", res.getTitle());
        verify(statusHistoryRepository).save(any(ReportStatusHistory.class));
    }

    @Test
    @DisplayName("Update report rejects non-owner modification")
    void testUpdateReportForbiddenForNonOwner() {
        ItemReport report = new ItemReport();
        report.setId(100L);
        report.setUser(owner);
        report.setStatus(ReportStatus.OPEN);

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        UpdateReportRequest req = new UpdateReportRequest();
        req.setTitle("Updated Title");

        assertThrows(ForbiddenException.class, () -> reportService.updateReport(100L, req, otherUser.getId(), false));
    }

    @Test
    @DisplayName("Owner can transition OPEN report to RESOLVED")
    void testOwnerCanMarkReportResolved() {
        ItemReport report = new ItemReport();
        report.setId(100L);
        report.setUser(owner);
        report.setStatus(ReportStatus.OPEN);
        report.setCategory(category);

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(reportRepository.save(any(ItemReport.class))).thenAnswer(i -> i.getArgument(0));

        UpdateReportStatusRequest req = new UpdateReportStatusRequest(ReportStatus.RESOLVED, "Found it under bed");
        ReportResponse res = reportService.updateReportStatus(100L, req, owner.getId(), false);

        assertNotNull(res);
        assertEquals(ReportStatus.RESOLVED, res.getStatus());
        verify(statusHistoryRepository).save(any(ReportStatusHistory.class));
    }

    @Test
    @DisplayName("Non-admin user cannot reopen a RESOLVED or CANCELLED report")
    void testCannotReopenClosedReport() {
        ItemReport report = new ItemReport();
        report.setId(100L);
        report.setUser(owner);
        report.setStatus(ReportStatus.RESOLVED);

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        UpdateReportStatusRequest req = new UpdateReportStatusRequest(ReportStatus.OPEN, "Reopen please");
        assertThrows(BadRequestException.class, () -> reportService.updateReportStatus(100L, req, owner.getId(), false));
    }

    @Test
    @DisplayName("Create report with optional photo attachment persists image and sets hasImage")
    void testCreateReportWithOptionalImage() {
        String testDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        CreateReportRequest req = new CreateReportRequest();
        req.setReportType(ReportType.FOUND);
        req.setTitle("Found Calculator");
        req.setDescription("Left on desk in 204");
        req.setCategoryId(10L);
        req.setLocationName("Lab 204");
        req.setIncidentDate(LocalDate.now());
        req.setImageData(testDataUrl);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        ItemReport saved = new ItemReport();
        saved.setId(105L);
        saved.setReportType(ReportType.FOUND);
        saved.setTitle("Found Calculator");
        saved.setDescription("Left on desk in 204");
        saved.setCategory(category);
        saved.setUser(owner);
        saved.setStatus(ReportStatus.OPEN);
        saved.setLocationName("Lab 204");
        saved.setIncidentDate(LocalDate.now());
        saved.setImageData(testDataUrl);

        when(reportRepository.save(any(ItemReport.class))).thenReturn(saved);

        ReportResponse res = reportService.createReport(req, 1L);

        assertNotNull(res);
        assertTrue(res.isHasImage());
        assertEquals("/api/reports/105/image", res.getImageUrl());
    }

    @Test
    @DisplayName("Create report without photo succeeds (photo is not mandatory)")
    void testCreateReportWithoutPhotoNotMandatory() {
        CreateReportRequest req = new CreateReportRequest();
        req.setReportType(ReportType.LOST);
        req.setTitle("Lost Sunglasses");
        req.setDescription("Black raybans");
        req.setCategoryId(10L);
        req.setLocationName("Dining hall");
        req.setIncidentDate(LocalDate.now());
        req.setImageData(null); // Explicitly omitted photo

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        ItemReport saved = new ItemReport();
        saved.setId(106L);
        saved.setReportType(ReportType.LOST);
        saved.setTitle("Lost Sunglasses");
        saved.setDescription("Black raybans");
        saved.setCategory(category);
        saved.setUser(owner);
        saved.setStatus(ReportStatus.OPEN);
        saved.setLocationName("Dining hall");
        saved.setIncidentDate(LocalDate.now());
        saved.setImageData(null);

        when(reportRepository.save(any(ItemReport.class))).thenReturn(saved);

        ReportResponse res = reportService.createReport(req, 1L);

        assertNotNull(res);
        assertFalse(res.isHasImage());
        assertNull(res.getImageUrl());
    }

    @Test
    @DisplayName("getReportImage returns image bytes and media type")
    void testGetReportImageSuccess() {
        String testDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        ItemReport report = new ItemReport();
        report.setId(100L);
        report.setImageData(testDataUrl);

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        var response = reportService.getReportImage(100L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }
}
