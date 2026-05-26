package com.resumescreener.resumescreener.controller;

import com.resumescreener.resumescreener.model.AIAnalysisResponse;
import com.resumescreener.resumescreener.model.InterviewEvaluationRequest;
import com.resumescreener.resumescreener.model.InterviewEvaluationResponse;
import com.resumescreener.resumescreener.model.InterviewQuestionsResponse;
import com.resumescreener.resumescreener.model.ResumeAnalysis;
import com.resumescreener.resumescreener.model.ResumeScreenResponse;
import com.resumescreener.resumescreener.repository.ResumeAnalysisRepository;
import com.resumescreener.resumescreener.service.AIService;
import com.resumescreener.resumescreener.util.FileParsingUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/screen")
public class ResumeController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    private final ObjectMapper objectMapper;

    private final AIService aiService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    public ResumeController(
        AIService aiService,
        ResumeAnalysisRepository resumeAnalysisRepository,
        ObjectMapper objectMapper
    ) {

        this.aiService = aiService;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<ResumeScreenResponse> screenResume(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("jobDescription") String jobDescription) {

        // Validate file
        if (file == null) {
            throw new IllegalArgumentException("Resume file is required");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Resume file cannot be empty");
        }

        // Validate job description
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException("Job description cannot be empty");
        }

        if (jobDescription.length() < 10 || jobDescription.length() > 5000) {
            throw new IllegalArgumentException("Job description must be between 10 and 5000 characters");
        }

        try {
            logger.info("Processing resume: {} for job description length: {}",
                    file.getOriginalFilename(), jobDescription.length());

            String extractedText = FileParsingUtil.extractTextFromMultipartFile(file);

            logger.debug("Extracted text length: {}", extractedText.length());

            // LLM Call 1: Analyze Resume
            AIAnalysisResponse analysis = aiService.analyzeResume(extractedText, jobDescription);

            String finalResultContent;
            String resultType;

            // Conditional Logic (IF/ELSE) based on match score
            if (analysis.getMatchScore() > 70) {
                // LLM Call 2: Generate Interview Questions
                InterviewQuestionsResponse questionsResponse =
                        aiService.generateInterviewQuestions(analysis);

                finalResultContent =
                        objectMapper.writeValueAsString(questionsResponse);
                resultType = "INTERVIEW_QUESTIONS";
            } else {
                // LLM Call 2: Generate Rejection Feedback
                finalResultContent = aiService.generateRejectionFeedback(analysis);
                resultType = "REJECTION_FEEDBACK";
            }

            // LLM Call 3: Generate HR Summary
            String hrSummary = aiService.generateHrSummary(analysis, finalResultContent);

            // Save to database
            ResumeAnalysis resumeAnalysis = new ResumeAnalysis(
                    null, // ID will be generated
                    file.getOriginalFilename(),
                    jobDescription,
                    analysis.getMatchScore(),
                    analysis, // Store the full AIAnalysisResponse
                    finalResultContent,
                    hrSummary,
                    null // createdAt will be set by @PrePersist
            );
            ResumeAnalysis savedAnalysis = resumeAnalysisRepository.save(resumeAnalysis);

            logger.info("Resume analysis saved with ID: {}, match score: {}",
                    savedAnalysis.getId(), analysis.getMatchScore());

            ResumeScreenResponse response = new ResumeScreenResponse(
                    savedAnalysis.getId(),
                    analysis.getMatchScore(),
                    resultType,
                    finalResultContent,
                    hrSummary
            );
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing resume", e);
            throw new RuntimeException("Error processing resume", e);
        }
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateAnswers(
            @RequestBody InterviewEvaluationRequest request
    ) {

        InterviewEvaluationResponse response =
                aiService.evaluateInterviewAnswers(request);

        return ResponseEntity.ok(response);
    }
}
