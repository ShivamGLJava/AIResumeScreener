package com.resumescreener.resumescreener.model;

import java.util.List;

public class AIAnalysisResponse {

    private List<String> skills;

    private String experience;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> missingRequirements;

    private int matchScore;

    public AIAnalysisResponse() {
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getMissingRequirements() {
        return missingRequirements;
    }

    public void setMissingRequirements(List<String> missingRequirements) {
        this.missingRequirements = missingRequirements;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    @Override
    public String toString() {
        return "AIAnalysisResponse{" +
                "skills=" + skills +
                ", experience='" + experience + '\'' +
                ", strengths=" + strengths +
                ", weaknesses=" + weaknesses +
                ", missingRequirements=" + missingRequirements +
                ", matchScore=" + matchScore +
                '}';
    }
}