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

@RestController
@RequestMapping("/api/v1/screen")
@CrossOrigin(origins = "*") // Allow requests from any origin for development purposes
public class ResumeController {

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
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription) {

        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            String extractedText = FileParsingUtil.extractTextFromMultipartFile(file);
            System.out.println("Extracted text from resume: " + extractedText.substring(0, Math.min(extractedText.length(), 500)) + "..."); // Log first 500 chars

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

            ResumeScreenResponse response = new ResumeScreenResponse(
                    savedAnalysis.getId(),
                    analysis.getMatchScore(),
                    resultType,
                    finalResultContent,
                    hrSummary
            );
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
