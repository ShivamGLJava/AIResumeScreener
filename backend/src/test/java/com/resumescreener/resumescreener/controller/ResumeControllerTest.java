package com.resumescreener.resumescreener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreener.resumescreener.config.GlobalExceptionHandler;
import com.resumescreener.resumescreener.model.AIAnalysisResponse;
import com.resumescreener.resumescreener.model.InterviewEvaluationRequest;
import com.resumescreener.resumescreener.model.InterviewEvaluationResponse;
import com.resumescreener.resumescreener.model.InterviewQuestionsResponse;
import com.resumescreener.resumescreener.model.ResumeAnalysis;
import com.resumescreener.resumescreener.repository.ResumeAnalysisRepository;
import com.resumescreener.resumescreener.service.AIService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Resume Controller Tests")
@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AIService aiService;

    @Mock
    private ResumeAnalysisRepository resumeAnalysisRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ResumeController resumeController;

    @BeforeEach
    void setUp() {
        this.resumeController = new ResumeController(aiService, resumeAnalysisRepository, objectMapper);
        this.mockMvc = MockMvcBuilders.standaloneSetup(resumeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should screen resume successfully with high match score and return interview questions")
    void screenResume_whenValidInputWithHighMatchScore_shouldReturnInterviewQuestions() throws Exception {
        MockMultipartFile resumeFile = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Java Developer with 5 years experience".getBytes()
        );
        String jobDescription = "Senior Java Developer with Spring Boot experience";

        AIAnalysisResponse analysis = new AIAnalysisResponse();
        analysis.setMatchScore(85);
        analysis.setExperience("5 years in Java development");
        analysis.setStrengths(Arrays.asList("Java expertise", "Spring Boot knowledge"));
        analysis.setWeaknesses(Collections.emptyList());

        InterviewQuestionsResponse questionsResponse = new InterviewQuestionsResponse();
        questionsResponse.setQuestions(Arrays.asList(
            "What are the latest features in Spring Boot 3.0?",
            "Explain your experience with microservices architecture"
        ));

        ResumeAnalysis savedAnalysis = new ResumeAnalysis(
            1L,
            "resume.pdf",
            jobDescription,
            85,
            analysis,
            "{\"questions\": []}",
            "Strong candidate for the position",
            LocalDateTime.now()
        );

        given(aiService.analyzeResume(anyString(), anyString())).willReturn(analysis);
        given(aiService.generateInterviewQuestions(analysis)).willReturn(questionsResponse);
        given(aiService.generateHrSummary(any(), anyString())).willReturn("Strong candidate for the position");
        given(resumeAnalysisRepository.save(any(ResumeAnalysis.class))).willReturn(savedAnalysis);

        mockMvc.perform(multipart("/api/v1/screen")
                .file(resumeFile)
                .param("jobDescription", jobDescription))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.analysisId").value(1L))
            .andExpect(jsonPath("$.matchScore").value(85))
            .andExpect(jsonPath("$.resultType").value("INTERVIEW_QUESTIONS"))
            .andExpect(jsonPath("$.hrSummary").value("Strong candidate for the position"));

        verify(aiService, times(1)).analyzeResume(anyString(), anyString());
        verify(aiService, times(1)).generateInterviewQuestions(analysis);
        verify(aiService, times(1)).generateHrSummary(any(), anyString());
    }

    @Test
    @DisplayName("Should screen resume with low match score and return rejection feedback")
    void screenResume_whenValidInputWithLowMatchScore_shouldReturnRejectionFeedback() throws Exception {
        MockMultipartFile resumeFile = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Entry level professional".getBytes()
        );
        String jobDescription = "Senior Java Developer with 10 years experience";

        AIAnalysisResponse analysis = new AIAnalysisResponse();
        analysis.setMatchScore(45);
        analysis.setExperience("Limited experience");
        analysis.setStrengths(Arrays.asList("Problem solving"));
        analysis.setWeaknesses(Arrays.asList("Insufficient Java experience", "No Spring Boot experience"));

        String rejectionFeedback = "While you have foundational skills, you lack the required senior-level experience.";

        ResumeAnalysis savedAnalysis = new ResumeAnalysis(
            2L,
            "resume.pdf",
            jobDescription,
            45,
            analysis,
            rejectionFeedback,
            "Candidate does not meet senior requirements",
            LocalDateTime.now()
        );

        given(aiService.analyzeResume(anyString(), anyString())).willReturn(analysis);
        given(aiService.generateRejectionFeedback(analysis)).willReturn(rejectionFeedback);
        given(aiService.generateHrSummary(any(), anyString())).willReturn("Candidate does not meet senior requirements");
        given(resumeAnalysisRepository.save(any(ResumeAnalysis.class))).willReturn(savedAnalysis);

        mockMvc.perform(multipart("/api/v1/screen")
                .file(resumeFile)
                .param("jobDescription", jobDescription))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.analysisId").value(2L))
            .andExpect(jsonPath("$.matchScore").value(45))
            .andExpect(jsonPath("$.resultType").value("REJECTION_FEEDBACK"))
            .andExpect(jsonPath("$.hrSummary").value("Candidate does not meet senior requirements"));

        verify(aiService, times(1)).analyzeResume(anyString(), anyString());
        verify(aiService, times(1)).generateRejectionFeedback(analysis);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when resume file is missing")
    void screenResume_whenFileIsMissing_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/screen")
                .param("jobDescription", "Senior Java Developer"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when resume file is empty")
    void screenResume_whenFileIsEmpty_shouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/screen")
                .file(emptyFile)
                .param("jobDescription", "Senior Java Developer"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when job description is missing")
    void screenResume_whenJobDescriptionIsMissing_shouldReturnBadRequest() throws Exception {
        MockMultipartFile resumeFile = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Resume content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/screen")
                .file(resumeFile))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 500 when resume parsing fails")
    void screenResume_whenFileParsingFails_shouldReturnInternalServerError() throws Exception {
        MockMultipartFile resumeFile = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Invalid resume content".getBytes()
        );
        String jobDescription = "Senior Java Developer";

        given(aiService.analyzeResume(anyString(), anyString()))
            .willThrow(new RuntimeException("PDF parsing error"));

        mockMvc.perform(multipart("/api/v1/screen")
                .file(resumeFile)
                .param("jobDescription", jobDescription))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should evaluate interview answers and return evaluation response")
    void evaluateAnswers_whenValidRequest_shouldReturnEvaluationResponse() throws Exception {
        InterviewEvaluationRequest request = new InterviewEvaluationRequest();
        request.setQuestions("[\"What are the latest features in Spring Boot?\", \"Explain microservices architecture\"]");
        request.setAnswers(Arrays.asList(
            "Spring Boot 3.0 has virtual threads, native compilation, etc.",
            "Microservices is an architectural approach for building distributed systems"
        ));

        InterviewEvaluationResponse response = new InterviewEvaluationResponse();
        response.setTechnicalScore(8);
        response.setCommunicationScore(8);
        response.setProblemSolvingScore(7);
        response.setOverallRating("Good");

        given(aiService.evaluateInterviewAnswers(any(InterviewEvaluationRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/screen/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallRating").value("Good"))
            .andExpect(jsonPath("$.technicalScore").value(8))
            .andExpect(jsonPath("$.communicationScore").value(8))
            .andExpect(jsonPath("$.problemSolvingScore").value(7));

        verify(aiService, times(1)).evaluateInterviewAnswers(any(InterviewEvaluationRequest.class));
    }

    @Test
    @DisplayName("Should handle boundary case with exactly 70 match score")
    void screenResume_whenMatchScoreIsExactly70_shouldReturnRejectionFeedback() throws Exception {
        MockMultipartFile resumeFile = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Borderline candidate".getBytes()
        );
        String jobDescription = "Senior Java Developer";

        AIAnalysisResponse analysis = new AIAnalysisResponse();
        analysis.setMatchScore(70);
        analysis.setExperience("Borderline experience");
        analysis.setStrengths(Arrays.asList("Java"));
        analysis.setWeaknesses(Arrays.asList("Missing advanced skills"));

        String rejectionFeedback = "Candidate is borderline but does not meet threshold.";

        ResumeAnalysis savedAnalysis = new ResumeAnalysis(
            3L,
            "resume.pdf",
            jobDescription,
            70,
            analysis,
            rejectionFeedback,
            "Borderline case",
            LocalDateTime.now()
        );

        given(aiService.analyzeResume(anyString(), anyString())).willReturn(analysis);
        given(aiService.generateRejectionFeedback(analysis)).willReturn(rejectionFeedback);
        given(aiService.generateHrSummary(any(), anyString())).willReturn("Borderline case");
        given(resumeAnalysisRepository.save(any(ResumeAnalysis.class))).willReturn(savedAnalysis);

        mockMvc.perform(multipart("/api/v1/screen")
                .file(resumeFile)
                .param("jobDescription", jobDescription))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("REJECTION_FEEDBACK"));
    }

    @Test
    @DisplayName("Should handle boundary case with match score above 70")
    void screenResume_whenMatchScoreIsAbove70_shouldReturnInterviewQuestions() throws Exception {
        MockMultipartFile resumeFile = new MockMultipartFile(
            "file",
            "resume.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "Strong candidate".getBytes()
        );
        String jobDescription = "Senior Java Developer";

        AIAnalysisResponse analysis = new AIAnalysisResponse();
        analysis.setMatchScore(71);
        analysis.setExperience("Good match");
        analysis.setStrengths(Arrays.asList("Java", "Spring Boot"));
        analysis.setWeaknesses(Collections.emptyList());

        InterviewQuestionsResponse questionsResponse = new InterviewQuestionsResponse();
        questionsResponse.setQuestions(Arrays.asList("Test question"));

        ResumeAnalysis savedAnalysis = new ResumeAnalysis(
            4L,
            "resume.pdf",
            jobDescription,
            71,
            analysis,
            "{}",
            "Good candidate",
            LocalDateTime.now()
        );

        given(aiService.analyzeResume(anyString(), anyString())).willReturn(analysis);
        given(aiService.generateInterviewQuestions(analysis)).willReturn(questionsResponse);
        given(aiService.generateHrSummary(any(), anyString())).willReturn("Good candidate");
        given(resumeAnalysisRepository.save(any(ResumeAnalysis.class))).willReturn(savedAnalysis);

        mockMvc.perform(multipart("/api/v1/screen")
                .file(resumeFile)
                .param("jobDescription", jobDescription))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultType").value("INTERVIEW_QUESTIONS"));
    }
}
