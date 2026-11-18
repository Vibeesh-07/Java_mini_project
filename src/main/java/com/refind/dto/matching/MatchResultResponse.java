package com.refind.dto.matching;

import com.refind.dto.report.ReportResponse;

import java.util.ArrayList;
import java.util.List;

public class MatchResultResponse {

    private ReportResponse report;
    private int matchScore;
    private List<MatchCriteriaScore> criteriaBreakdown = new ArrayList<>();
    private List<String> explanations = new ArrayList<>();

    public MatchResultResponse() {
    }

    public MatchResultResponse(ReportResponse report, int matchScore, List<MatchCriteriaScore> criteriaBreakdown, List<String> explanations) {
        this.report = report;
        this.matchScore = matchScore;
        this.criteriaBreakdown = criteriaBreakdown != null ? criteriaBreakdown : new ArrayList<>();
        this.explanations = explanations != null ? explanations : new ArrayList<>();
    }

    public ReportResponse getReport() {
        return report;
    }

    public void setReport(ReportResponse report) {
        this.report = report;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public List<MatchCriteriaScore> getCriteriaBreakdown() {
        return criteriaBreakdown;
    }

    public void setCriteriaBreakdown(List<MatchCriteriaScore> criteriaBreakdown) {
        this.criteriaBreakdown = criteriaBreakdown;
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public void setExplanations(List<String> explanations) {
        this.explanations = explanations;
    }
}
