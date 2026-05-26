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
                """;

        String userPrompt =
                "Resume:\n" + resumeText +
                "\n\nJob Description:\n" + jobDescription;

        String responseJson = callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();

        System.out.println("RAW AI RESPONSE:");
        System.out.println(responseJson);

        try {
            return objectMapper.readValue(
                    responseJson,
                    AIAnalysisResponse.class
            );

        } catch (JsonProcessingException e) {

            System.out.println("FAILED TO PARSE JSON:");
            System.out.println(responseJson);

            throw new RuntimeException(
                    "Failed to parse AI response",
                    e
            );
        }
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

                Return plain text only.
                """;

        String userPrompt =
                "Resume Analysis:\n" + analysis.toString();

        return callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();
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

                Return plain text only.
                """;

        String userPrompt =
                "Resume Analysis:\n" + analysis.toString() +
                "\n\nFinal Result:\n" + finalResult;

        return callHuggingFaceApi(
                systemPrompt,
                userPrompt,
                RESUME_ANALYSIS_MODEL
        ).block();
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

                Evaluate the candidate answers.

                Return ONLY valid JSON.

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

            return objectMapper.readValue(
                    responseJson,
                    InterviewEvaluationResponse.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to evaluate answers",
                    e
            );
        }
    }
}