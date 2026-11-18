package com.refind.matching;

import com.refind.dto.matching.MatchCriteriaScore;
import com.refind.dto.matching.MatchResultResponse;
import com.refind.dto.report.ReportResponse;
import com.refind.entity.ItemReport;
import com.refind.entity.ReportStatus;
import com.refind.entity.ReportType;
import com.refind.repository.ItemReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MatchingService {

    private final ItemReportRepository reportRepository;
    private final int minScoreThreshold;

    public MatchingService(
            ItemReportRepository reportRepository,
            @Value("${app.matching.min-score-threshold:25}") int minScoreThreshold
    ) {
        this.reportRepository = reportRepository;
        this.minScoreThreshold = minScoreThreshold;
    }

    /**
     * Find potential matching reports for a given source report.
     * Matches LOST with FOUND reports only, in open or pending status.
     */
    @Transactional(readOnly = true)
    public List<MatchResultResponse> findMatches(ItemReport sourceReport, Long currentUserId) {
        if (sourceReport == null || sourceReport.getStatus() == ReportStatus.RESOLVED || sourceReport.getStatus() == ReportStatus.CANCELLED) {
            return Collections.emptyList();
        }

        // Compare LOST with FOUND only
        ReportType targetType = (sourceReport.getReportType() == ReportType.LOST) ? ReportType.FOUND : ReportType.LOST;

        // Fetch candidate reports of opposite type that are unresolved
        List<ItemReport> candidatesOpen = reportRepository.findByReportTypeAndStatus(targetType, ReportStatus.OPEN);
        List<ItemReport> candidatesPending = reportRepository.findByReportTypeAndStatus(targetType, ReportStatus.CLAIM_PENDING);

        List<ItemReport> candidates = new ArrayList<>(candidatesOpen);
        candidates.addAll(candidatesPending);

        List<MatchResultResponse> results = new ArrayList<>();

        for (ItemReport candidate : candidates) {
            // Cannot match with self
            if (candidate.getId().equals(sourceReport.getId())) {
                continue;
            }

            MatchResultResponse matchResult = calculateMatch(sourceReport, candidate, currentUserId);
            if (matchResult.getMatchScore() >= minScoreThreshold) {
                results.add(matchResult);
            }
        }

        // Sort descending by score, then by newest incident date
        results.sort((a, b) -> {
            int scoreCmp = Integer.compare(b.getMatchScore(), a.getMatchScore());
            if (scoreCmp != 0) return scoreCmp;
            LocalDate d1 = a.getReport().getIncidentDate();
            LocalDate d2 = b.getReport().getIncidentDate();
            if (d1 != null && d2 != null) {
                return d2.compareTo(d1);
            }
            return 0;
        });

        return results;
    }

    /**
     * Deterministically calculates match score between two reports (max 100 points).
     */
    public MatchResultResponse calculateMatch(ItemReport report1, ItemReport report2, Long currentUserId) {
        int totalScore = 0;
        List<MatchCriteriaScore> criteriaScores = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        // 1. Category Matching (up to 30 points)
        int categoryScore = 0;
        String catExpl = "Category mismatch or missing";
        if (report1.getCategory() != null && report2.getCategory() != null
                && Objects.equals(report1.getCategory().getId(), report2.getCategory().getId())) {
            categoryScore = 30;
            catExpl = "Identical category: " + report1.getCategory().getName() + " (+30 pts)";
            explanations.add(catExpl);
        }
        criteriaScores.add(new MatchCriteriaScore("Category Match", categoryScore, 30, catExpl));
        totalScore += categoryScore;

        // 2. Title & Description Keyword Overlap (up to 25 points)
        List<String> textExpls = new ArrayList<>();
        int textScore = TextSimilarityUtil.calculateTextSimilarityScore(
                report1.getTitle(), report1.getDescription(),
                report2.getTitle(), report2.getDescription(),
                textExpls
        );
        String textExpl = textExpls.isEmpty() ? "No significant keyword overlap" : String.join("; ", textExpls);
        explanations.addAll(textExpls);
        criteriaScores.add(new MatchCriteriaScore("Title & Description Keywords", textScore, 25, textExpl));
        totalScore += textScore;

        // 3. Color & Brand Matching (up to 20 points: Color 12, Brand 8)
        int colorScore = 0;
        int brandScore = 0;
        List<String> attributeExpls = new ArrayList<>();

        // Color match (up to 12 points)
        if (hasValue(report1.getColor()) && hasValue(report2.getColor())) {
            String c1 = report1.getColor().trim().toLowerCase(Locale.ROOT);
            String c2 = report2.getColor().trim().toLowerCase(Locale.ROOT);
            if (c1.equals(c2)) {
                colorScore = 12;
                attributeExpls.add("Exact color match: '" + report1.getColor() + "' (+12 pts)");
            } else if (c1.contains(c2) || c2.contains(c1)) {
                colorScore = 8;
                attributeExpls.add("Partial color match: '" + report1.getColor() + "' / '" + report2.getColor() + "' (+8 pts)");
            }
        }

        // Brand match (up to 8 points)
        if (hasValue(report1.getBrand()) && hasValue(report2.getBrand())) {
            String b1 = report1.getBrand().trim().toLowerCase(Locale.ROOT);
            String b2 = report2.getBrand().trim().toLowerCase(Locale.ROOT);
            if (b1.equals(b2)) {
                brandScore = 8;
                attributeExpls.add("Exact brand match: '" + report1.getBrand() + "' (+8 pts)");
            } else if (b1.contains(b2) || b2.contains(b1)) {
                brandScore = 5;
                attributeExpls.add("Partial brand match: '" + report1.getBrand() + "' / '" + report2.getBrand() + "' (+5 pts)");
            }
        }

        int colorBrandTotal = Math.min(20, colorScore + brandScore);
        String colorBrandExpl = attributeExpls.isEmpty() ? "No matching color or brand recorded" : String.join("; ", attributeExpls);
        explanations.addAll(attributeExpls);
        criteriaScores.add(new MatchCriteriaScore("Color & Brand", colorBrandTotal, 20, colorBrandExpl));
        totalScore += colorBrandTotal;

        // 4. Campus Location Proximity (up to 15 points)
        List<String> locExpls = new ArrayList<>();
        int locScore = CampusLocation.calculateLocationScore(
                report1.getLocationName(), report1.getLocationZone(),
                report2.getLocationName(), report2.getLocationZone(),
                locExpls
        );
        String locExpl = locExpls.isEmpty() ? "Different campus locations" : String.join("; ", locExpls);
        explanations.addAll(locExpls);
        criteriaScores.add(new MatchCriteriaScore("Location Proximity", locScore, 15, locExpl));
        totalScore += locScore;

        // 5. Incident Date Proximity (up to 10 points)
        int dateScore = 0;
        String dateExpl = "Incident dates differ significantly";
        if (report1.getIncidentDate() != null && report2.getIncidentDate() != null) {
            long daysApart = Math.abs(ChronoUnit.DAYS.between(report1.getIncidentDate(), report2.getIncidentDate()));
            if (daysApart == 0) {
                dateScore = 10;
                dateExpl = "Same incident date: " + report1.getIncidentDate() + " (+10 pts)";
                explanations.add(dateExpl);
            } else if (daysApart == 1) {
                dateScore = 9;
                dateExpl = "Within 1 day (" + daysApart + " day difference) (+9 pts)";
                explanations.add(dateExpl);
            } else if (daysApart <= 3) {
                dateScore = 7;
                dateExpl = "Within 3 days (" + daysApart + " days difference) (+7 pts)";
                explanations.add(dateExpl);
            } else if (daysApart <= 7) {
                dateScore = 5;
                dateExpl = "Within 1 week (" + daysApart + " days difference) (+5 pts)";
                explanations.add(dateExpl);
            } else if (daysApart <= 14) {
                dateScore = 3;
                dateExpl = "Within 2 weeks (" + daysApart + " days difference) (+3 pts)";
                explanations.add(dateExpl);
            } else {
                dateExpl = "Reports are " + daysApart + " days apart (0 pts)";
            }
        }
        criteriaScores.add(new MatchCriteriaScore("Date Proximity", dateScore, 10, dateExpl));
        totalScore += dateScore;

        ReportResponse candidateDto = ReportResponse.fromEntity(report2, currentUserId);
        return new MatchResultResponse(candidateDto, totalScore, criteriaScores, explanations);
    }

    private boolean hasValue(String str) {
        return str != null && !str.trim().isBlank();
    }
}
