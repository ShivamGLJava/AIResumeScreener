package com.resumescreener.resumescreener.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeScreenResponse {
    private Long analysisId;
    private Integer matchScore;
    private String resultType; // e.g., "INTERVIEW_QUESTIONS", "REJECTION_FEEDBACK"
    private String resultContent;
    private String hrSummary;
}
