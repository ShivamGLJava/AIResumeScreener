package com.resumescreener.resumescreener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreener.resumescreener.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service("huggingFaceAIService")
public class HuggingFaceAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceAIService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.key}")
    private String huggingFaceApiKey;

    private static final String HUGGINGFACE_API_URL =
            "https://router.huggingface.co/v1/chat/completions";

    // Use stable/public models
    private static final String RESUME_ANALYSIS_MODEL =
            "meta-llama/Llama-3.1-8B-Instruct";

    private static final String INTERVIEW_QUESTION_MODEL =
            "Qwen/Qwen2.5-7B-Instruct";

    public HuggingFaceAIService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClientBuilder
                .baseUrl(HUGGINGFACE_API_URL)
                .build();

        this.objectMapper = objectMapper;
    }

    @Override
    public AIAnalysisResponse analyzeResume(
            String resumeText,
            String jobDescription
    ) {

        // Step 1: Remove personal information (PII)
        String cleanedResumeText = com.resumescreener.resumescreener.util.ResumeDataExtractor
                .extractCleanResumeData(resumeText);

        logger.debug("Privacy check: original length={}, cleaned length={}",
                resumeText.length(), cleanedResumeText.length());

        // Step 2: Extract only relevant data (skills, experience, projects)
        java.util.Map<String, Object> relevantData = com.resumescreener.resumescreener.util.ResumeDataExtractor
                .extractRelevantData(cleanedResumeText);

        logger.debug("Extracted data: skills={}, education={}, projects={}",
                relevantData.get("skills"), relevantData.get("education"), relevantData.get("projects"));

        String systemPrompt = """
                You are an expert recruiter evaluating resume fit for a job position.

                Return ONLY valid JSON. No markdown, no explanations, no text before/after JSON.

                MATCH SCORE CALCULATION (0-100):
                - 90-100: Excellent match - has all required skills + relevant experience
                - 75-89: Strong match - has most required skills + good experience
                - 60-74: Good match - has core skills but missing some requirements
                - 45-59: Fair match - has some relevant skills, limited experience
                - 30-44: Partial match - some transferable skills, mostly junior
                - 0-29: Weak match - lacks required skills/experience

                IMPORTANT SCORING RULES:
                1. Internship experience counts as professional experience
                2. Major tech companies (Google, Amazon, Meta, Microsoft) = highly relevant
                3. Each matched programming language = +5 to +10 points
                4. Each matched framework/tool = +3 to +5 points
                5. Each year of relevant experience = +3 to +5 points
                6. Relevant certification = +5 points
                7. Zero matched requirements = matchScore capped at 20 maximum

                Required JSON format:
                {
                  "skills": ["skill1", "skill2"],
                  "experience": "summary of relevant experience",
                  "strengths": ["strength1", "strength2"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "missingRequirements": ["requirement1", "requirement2"],
                  "matchScore": 75
                }

                Analyze resume against job description fairly.
                Focus on: technical skills, experience level, relevant projects, education.
                Be generous with scoring for relevant internships and junior positions.
                """;

        String userPrompt =
                "Resume Data (PII Removed):\n" + relevantData +
                "\n\nJob Description:\n" + jobDescription;

        String responseJson = callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();

        logger.debug("LLM response received, parsing...");

        try {
            AIAnalysisResponse response = objectMapper.readValue(
                    responseJson,
                    AIAnalysisResponse.class
            );

            // Step 3: Validate AI output for safety (hate speech, bias, etc.)
            com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                    validateAnalysisResponse(response);

            if (!validation.isValid()) {
                logger.warn("Safety validation failed: {}", validation.getSummary());
                throw new RuntimeException("Output failed safety validation: " + validation.getSummary());
            }

            logger.debug("Safety validation passed");

            return response;

        } catch (JsonProcessingException e) {

            System.out.println("FAILED TO PARSE JSON:");
            System.out.println(responseJson);

            throw new RuntimeException(
                    "Failed to parse AI response",
                    e
            );
        }
    }

    private com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult
            validateAnalysisResponse(AIAnalysisResponse response) {

        StringBuilder outputText = new StringBuilder();
        outputText.append(String.join(", ", response.getStrengths())).append(" ");
        outputText.append(String.join(", ", response.getWeaknesses())).append(" ");
        outputText.append(String.join(", ", response.getMissingRequirements())).append(" ");
        outputText.append(response.getExperience());

        com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                com.resumescreener.resumescreener.util.OutputSafetyValidator.validateOutput(outputText.toString());

        return validation;
    }

    @Override
    public InterviewQuestionsResponse generateInterviewQuestions(
            AIAnalysisResponse analysis
    ) {

        String systemPrompt = """
            You are a strict JSON generator.

            Return ONLY valid JSON.

            DO NOT use markdown.
            DO NOT explain.
            DO NOT truncate output.

            You MUST return EXACTLY this schema:

            {
            "questions": [
                "question 1",
                "question 2",
                "question 3"
            ]
            }

            Generate exactly 3 technical interview questions.
            """;

        String userPrompt =
                "Resume Analysis:\n" + analysis.toString();

        String responseJson = callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                INTERVIEW_QUESTION_MODEL
        ).block();

        logger.debug("Interview questions response received");

        try {

            return objectMapper.readValue(
                    responseJson,
                    InterviewQuestionsResponse.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse interview questions",
                    e
            );
        }
    }

    @Override
    public String generateRejectionFeedback(
            AIAnalysisResponse analysis
    ) {

        String systemPrompt = """
                Generate professional HR rejection feedback.

                Include:
                - Positive tone
                - Missing skills
                - Improvement suggestions

                IMPORTANT:
                - Be respectful and constructive
                - Do NOT use discriminatory language
                - Do NOT make personal judgments
                - Focus on skills and experience gaps only

                Return plain text only.
                """;

        String userPrompt =
                "Resume Analysis:\n" + analysis.toString();

        String feedback = callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();

        // Validate and sanitize output
        com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                com.resumescreener.resumescreener.util.OutputSafetyValidator.validateOutput(feedback);

        if (!validation.isValid()) {
            logger.warn("Rejection feedback safety check failed: {}", validation.getSummary());
            throw new RuntimeException("Rejection feedback failed safety validation");
        }

        logger.debug("Rejection feedback safety check passed");

        return com.resumescreener.resumescreener.util.OutputSafetyValidator.sanitizeOutput(feedback);
    }

    @Override
    public String generateHrSummary(
            AIAnalysisResponse analysis,
            String finalResult
    ) {

        String systemPrompt = """
                Generate concise HR summary.

                Include:
                - Match score
                - Key strengths
                - Weaknesses
                - Final recommendation

                IMPORTANT:
                - Be objective and fair
                - Do NOT use discriminatory language
                - Do NOT make personal judgments about the candidate
                - Focus only on professional qualifications

                Return plain text only.
                """;

        String userPrompt =
                "Resume Analysis:\n" + analysis.toString() +
                "\n\nFinal Result:\n" + finalResult;

        String summary = callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();

        // Validate and sanitize output
        com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                com.resumescreener.resumescreener.util.OutputSafetyValidator.validateOutput(summary);

        if (!validation.isValid()) {
            logger.warn("HR summary safety check failed: {}", validation.getSummary());
            throw new RuntimeException("HR summary failed safety validation");
        }

        logger.debug("HR summary safety check passed");

        return com.resumescreener.resumescreener.util.OutputSafetyValidator.sanitizeOutput(summary);
    }

    private Mono<String> callHuggingFaceApi(
            String systemPrompt,
            String userPrompt,
            String model
    ) {

        logger.debug("LLM API call initiated with model: {}", model);

        Message systemMessage =
                new Message("system", systemPrompt);

        Message userMessage =
                new Message("user", userPrompt);

        HuggingFaceChatRequest request =
                new HuggingFaceChatRequest(
                        model,
                        java.util.Arrays.asList(
                                systemMessage,
                                userMessage
                        )
                );

        return webClient.post()
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + huggingFaceApiKey
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()

                // Better error handling
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {

                                    logger.error("HF API error response received");

                                    return Mono.error(
                                            new RuntimeException(
                                                    errorBody
                                            )
                                    );
                                })
                )

                .bodyToMono(HuggingFaceChatResponse.class)

                .map(response -> {

                    if (response != null &&
                        response.getChoices() != null &&
                        !response.getChoices().isEmpty()) {

                        return response
                                .getChoices()
                                .get(0)
                                .getMessage()
                                .getContent();
                    }

                    return """
                            {
                              "skills": [],
                              "experience": "",
                              "strengths": [],
                              "weaknesses": [],
                              "missingRequirements": [],
                              "matchScore": 0
                            }
                            """;
                });
    }

    @Override
    public InterviewEvaluationResponse evaluateInterviewAnswers(
            InterviewEvaluationRequest request
    ) {

        String systemPrompt = """
                You are an expert technical interviewer.

                Evaluate the candidate answers FAIRLY and OBJECTIVELY.

                Return ONLY valid JSON. No markdown, no explanations, no text before/after JSON.

                SCORING RULES (ALL SCORES ARE 0-10):
                - technicalScore: 0-10 (knowledge, depth, accuracy)
                - communicationScore: 0-10 (clarity, articulation, coherence)
                - problemSolvingScore: 0-10 (logic, approach, completeness)

                SCORE INTERPRETATION:
                - 9-10: Excellent
                - 7-8: Very Good
                - 5-6: Good
                - 3-4: Fair
                - 0-2: Poor

                IMPORTANT RULES:
                - All scores MUST be between 0 and 10 (inclusive)
                - Score based ONLY on technical knowledge and communication
                - Do NOT make any personal judgments or assumptions
                - Do NOT use discriminatory language
                - Be fair and unbiased in your evaluation
                - Focus on the quality of answers, not the person

                Required JSON format:

                {
                  "overallRating": "Very Good",
                  "technicalScore": 8,
                  "communicationScore": 7,
                  "problemSolvingScore": 8,
                  "evaluatorSummary": "Brief summary here",
                  "strengths": ["strength1", "strength2"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "recommendation": "Recommendation here"
                }
                """;

        String userPrompt =
                "Questions:\n" +
                request.getQuestions() +
                "\n\nAnswers:\n" +
                request.getAnswers();

        String responseJson =
                callHuggingFaceApi(
                        systemPrompt,
                        userPrompt,
                        RESUME_ANALYSIS_MODEL
                ).block();

        try {
            InterviewEvaluationResponse response = objectMapper.readValue(
                    responseJson,
                    InterviewEvaluationResponse.class
            );

            // Validate interview evaluation output
            com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                    validateInterviewResponse(response);

            if (!validation.isValid()) {
                logger.warn("Interview evaluation safety check failed: {}", validation.getSummary());
                throw new RuntimeException("Interview evaluation failed safety validation");
            }

            logger.debug("Interview evaluation safety check passed");

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to evaluate answers",
                    e
            );
        }
    }

    private com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult
            validateInterviewResponse(InterviewEvaluationResponse response) {

        StringBuilder outputText = new StringBuilder();
        outputText.append(response.getOverallRating()).append(" ");
        outputText.append(response.getEvaluatorSummary()).append(" ");
        outputText.append(String.join(", ", response.getStrengths())).append(" ");
        outputText.append(String.join(", ", response.getWeaknesses())).append(" ");
        outputText.append(response.getRecommendation());

        com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                com.resumescreener.resumescreener.util.OutputSafetyValidator.validateOutput(outputText.toString());

        return validation;
    }
}