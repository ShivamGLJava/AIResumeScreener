package com.resumescreener.resumescreener.service;

import com.resumescreener.resumescreener.model.AIAnalysisResponse;
import com.resumescreener.resumescreener.model.InterviewEvaluationRequest;
import com.resumescreener.resumescreener.model.InterviewEvaluationResponse;
import com.resumescreener.resumescreener.model.InterviewQuestionsResponse;

public interface AIService {
    AIAnalysisResponse analyzeResume(String resumeText, String jobDescription);
    InterviewQuestionsResponse generateInterviewQuestions(
        AIAnalysisResponse analysis
    );
    String generateRejectionFeedback(AIAnalysisResponse analysis);
    String generateHrSummary(AIAnalysisResponse analysis, String finalResult);
    InterviewEvaluationResponse evaluateInterviewAnswers(
        InterviewEvaluationRequest request
    );
}
