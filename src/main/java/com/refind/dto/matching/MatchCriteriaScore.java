package com.refind.dto.matching;

public class MatchCriteriaScore {

    private String criteriaName;
    private int pointsEarned;
    private int maxPoints;
    private String explanation;

    public MatchCriteriaScore() {
    }

    public MatchCriteriaScore(String criteriaName, int pointsEarned, int maxPoints, String explanation) {
        this.criteriaName = criteriaName;
        this.pointsEarned = pointsEarned;
        this.maxPoints = maxPoints;
        this.explanation = explanation;
    }

    public String getCriteriaName() {
        return criteriaName;
    }

    public void setCriteriaName(String criteriaName) {
        this.criteriaName = criteriaName;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
