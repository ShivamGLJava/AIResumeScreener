package com.resumescreener.resumescreener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreener.resumescreener.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service("huggingFaceAIService")
public class HuggingFaceAIService implements AIService {

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

        System.out.println("=== PRIVACY CHECK ===");
        System.out.println("Original resume length: " + resumeText.length());
        System.out.println("Cleaned resume length: " + cleanedResumeText.length());

        // Step 2: Extract only relevant data (skills, experience, projects)
        java.util.Map<String, Object> relevantData = com.resumescreener.resumescreener.util.ResumeDataExtractor
                .extractRelevantData(cleanedResumeText);

        System.out.println("=== EXTRACTED DATA ===");
        System.out.println("Skills: " + relevantData.get("skills"));
        System.out.println("Education: " + relevantData.get("education"));
        System.out.println("Projects: " + relevantData.get("projects"));

        String systemPrompt = """
                Return ONLY valid JSON.

                Do not include markdown.
                Do not include explanations.
                Do not include text before or after JSON.

                Required JSON format:

                {
                  "skills": [],
                  "experience": "",
                  "strengths": [],
                  "weaknesses": [],
                  "missingRequirements": [],
                  "matchScore": 0
                }

                Analyze the resume against the job description.
                Focus ONLY on technical skills, experience, education, and projects.
                """;

        String userPrompt =
                "Resume Data (PII Removed):\n" + relevantData +
                "\n\nJob Description:\n" + jobDescription;

        String responseJson = callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();

        System.out.println("RAW AI RESPONSE:");
        System.out.println(responseJson);

        try {
            AIAnalysisResponse response = objectMapper.readValue(
                    responseJson,
                    AIAnalysisResponse.class
            );

            // Step 3: Validate AI output for safety (hate speech, bias, etc.)
            com.resumescreener.resumescreener.util.OutputSafetyValidator.ValidationResult validation =
                    validateAnalysisResponse(response);

            if (!validation.isValid()) {
                System.out.println("=== SAFETY VALIDATION FAILED ===");
                System.out.println(validation.getSummary());
                throw new RuntimeException("Output failed safety validation: " + validation.getSummary());
            }

            System.out.println("=== SAFETY VALIDATION PASSED ===");
            System.out.println(validation.getSummary());

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

        System.out.println("INTERVIEW QUESTIONS RAW RESPONSE:");
        System.out.println(responseJson);

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
            System.out.println("=== REJECTION FEEDBACK SAFETY CHECK FAILED ===");
            System.out.println(validation.getSummary());
            throw new RuntimeException("Rejection feedback failed safety validation");
        }

        System.out.println("=== REJECTION FEEDBACK SAFETY CHECK PASSED ===");

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
            System.out.println("=== HR SUMMARY SAFETY CHECK FAILED ===");
            System.out.println(validation.getSummary());
            throw new RuntimeException("HR summary failed safety validation");
        }

        System.out.println("=== HR SUMMARY SAFETY CHECK PASSED ===");

        return com.resumescreener.resumescreener.util.OutputSafetyValidator.sanitizeOutput(summary);
    }

    private Mono<String> callHuggingFaceApi(
            String systemPrompt,
            String userPrompt,
            String model
    ) {

        System.out.println("=================================");
        System.out.println("USING MODEL: " + model);
        System.out.println("HF API KEY PRESENT: " +
                (huggingFaceApiKey != null &&
                 !huggingFaceApiKey.isBlank()));
        System.out.println("=================================");

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

                                    System.out.println(
                                            "HF API ERROR RESPONSE:"
                                    );

                                    System.out.println(errorBody);

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

                Return ONLY valid JSON.

                IMPORTANT RULES:
                - Score based ONLY on technical knowledge and communication
                - Do NOT make any personal judgments or assumptions
                - Do NOT use discriminatory language
                - Be fair and unbiased in your evaluation
                - Focus on the quality of answers, not the person

                Required format:

                {
                "overallRating": "",
                "technicalScore": 0,
                "communicationScore": 0,
                "problemSolvingScore": 0,
                "evaluatorSummary": "",
                "strengths": [],
                "weaknesses": [],
                "recommendation": ""
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
                System.out.println("=== INTERVIEW EVALUATION SAFETY CHECK FAILED ===");
                System.out.println(validation.getSummary());
                throw new RuntimeException("Interview evaluation failed safety validation");
            }

            System.out.println("=== INTERVIEW EVALUATION SAFETY CHECK PASSED ===");

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