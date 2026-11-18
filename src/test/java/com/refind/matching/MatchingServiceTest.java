package com.refind.matching;

import com.refind.dto.matching.MatchResultResponse;
import com.refind.entity.*;
import com.refind.repository.ItemReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private ItemReportRepository reportRepository;

    private MatchingService matchingService;

    private Category electronicsCategory;
    private Category booksCategory;
    private User student1;
    private User student2;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(reportRepository, 25);

        electronicsCategory = new Category("Electronics", "Gadgets", "laptop");
        electronicsCategory.setId(1L);

        booksCategory = new Category("Books", "Books and notebooks", "book-open");
        booksCategory.setId(2L);

        student1 = new User("Alice", "alice@campus.edu", "hashed1", Role.ROLE_USER);
        student1.setId(10L);

        student2 = new User("Bob", "bob@campus.edu", "hashed2", Role.ROLE_USER);
        student2.setId(20L);
    }

    @Test
    @DisplayName("High match score when category, title, color, brand, location, and date align")
    void testHighConfidenceMatch() {
        ItemReport lostReport = new ItemReport();
        lostReport.setId(101L);
        lostReport.setReportType(ReportType.LOST);
        lostReport.setTitle("Black Casio Calculator fx-991EX");
        lostReport.setDescription("Lost my scientific calculator during morning exam");
        lostReport.setCategory(electronicsCategory);
        lostReport.setColor("Black");
        lostReport.setBrand("Casio");
        lostReport.setLocationName("Science Lab 301");
        lostReport.setLocationZone("SCIENCE_BUILDING");
        lostReport.setIncidentDate(LocalDate.now());
        lostReport.setUser(student1);

        ItemReport foundReport = new ItemReport();
        foundReport.setId(202L);
        foundReport.setReportType(ReportType.FOUND);
        foundReport.setTitle("Black Casio Scientific Calculator");
        foundReport.setDescription("Found a Casio calculator on desk after lab exam");
        foundReport.setCategory(electronicsCategory);
        foundReport.setColor("Black");
        foundReport.setBrand("Casio");
        foundReport.setLocationName("Science Lab 301");
        foundReport.setLocationZone("SCIENCE_BUILDING");
        foundReport.setIncidentDate(LocalDate.now());
        foundReport.setUser(student2);

        MatchResultResponse result = matchingService.calculateMatch(lostReport, foundReport, student1.getId());

        assertNotNull(result);
        assertTrue(result.getMatchScore() >= 80, "Expected match score >= 80, got: " + result.getMatchScore());
        assertEquals(5, result.getCriteriaBreakdown().size());
        assertTrue(result.getExplanations().size() >= 3);
    }

    @Test
    @DisplayName("Zero score for missing fields, never treating null as a match")
    void testMissingFieldsContributeZeroPoints() {
        ItemReport lostReport = new ItemReport();
        lostReport.setId(101L);
        lostReport.setReportType(ReportType.LOST);
        lostReport.setTitle("Umbrella");
        lostReport.setDescription("Plain umbrella");
        lostReport.setCategory(electronicsCategory);
        lostReport.setColor(null); // missing
        lostReport.setBrand(null); // missing
        lostReport.setLocationName("Main Gate");
        lostReport.setLocationZone("CAMPUS_GENERAL");
        lostReport.setIncidentDate(LocalDate.now());
        lostReport.setUser(student1);

        ItemReport foundReport = new ItemReport();
        foundReport.setId(202L);
        foundReport.setReportType(ReportType.FOUND);
        foundReport.setTitle("Umbrella");
        foundReport.setDescription("Left on bench");
        foundReport.setCategory(booksCategory); // different category
        foundReport.setColor(null); // missing
        foundReport.setBrand(null); // missing
        foundReport.setLocationName("Sports Arena Gym");
        foundReport.setLocationZone("SPORTS_ARENA");
        foundReport.setIncidentDate(LocalDate.now().minusDays(45)); // far date
        foundReport.setUser(student2);

        MatchResultResponse result = matchingService.calculateMatch(lostReport, foundReport, student1.getId());

        // Category is different -> 0 pts
        // Color & Brand missing -> 0 pts
        // Locations different and not adjacent -> 0 pts
        // Date 45 days apart -> 0 pts
        // Only text overlap for "Umbrella"
        assertTrue(result.getMatchScore() < 25, "Expected low match score for divergent reports, got: " + result.getMatchScore());
    }

    @Test
    @DisplayName("findMatches compares LOST only with eligible FOUND reports and filters by threshold")
    void testFindMatchesFiltersByThresholdAndOppositeType() {
        ItemReport lostReport = new ItemReport();
        lostReport.setId(101L);
        lostReport.setReportType(ReportType.LOST);
        lostReport.setTitle("Blue Dell Laptop Charger");
        lostReport.setDescription("65W Dell USB-C charger with blue sticker");
        lostReport.setCategory(electronicsCategory);
        lostReport.setColor("Blue");
        lostReport.setBrand("Dell");
        lostReport.setLocationName("Main Library 2nd Floor");
        lostReport.setLocationZone("LIBRARY_COMPLEX");
        lostReport.setIncidentDate(LocalDate.now());
        lostReport.setStatus(ReportStatus.OPEN);
        lostReport.setUser(student1);

        ItemReport candidateFound = new ItemReport();
        candidateFound.setId(202L);
        candidateFound.setReportType(ReportType.FOUND);
        candidateFound.setTitle("Dell USB-C Charger");
        candidateFound.setDescription("Found a Dell laptop charger on 2nd floor desk");
        candidateFound.setCategory(electronicsCategory);
        candidateFound.setColor("Blue");
        candidateFound.setBrand("Dell");
        candidateFound.setLocationName("Main Library 2nd Floor");
        candidateFound.setLocationZone("LIBRARY_COMPLEX");
        candidateFound.setIncidentDate(LocalDate.now());
        candidateFound.setStatus(ReportStatus.OPEN);
        candidateFound.setUser(student2);

        when(reportRepository.findByReportTypeAndStatus(ReportType.FOUND, ReportStatus.OPEN))
                .thenReturn(List.of(candidateFound));
        when(reportRepository.findByReportTypeAndStatus(ReportType.FOUND, ReportStatus.CLAIM_PENDING))
                .thenReturn(List.of());

        List<MatchResultResponse> matches = matchingService.findMatches(lostReport, student1.getId());

        assertNotNull(matches);
        assertEquals(1, matches.size());
        assertEquals(202L, matches.get(0).getReport().getId());
        assertTrue(matches.get(0).getMatchScore() >= 25);
    }
}
