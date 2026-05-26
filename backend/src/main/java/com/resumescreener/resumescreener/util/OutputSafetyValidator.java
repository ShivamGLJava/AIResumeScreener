package com.resumescreener.resumescreener.util;

import java.util.*;
import java.util.regex.Pattern;

public class OutputSafetyValidator {

    // Hate speech and offensive content patterns
    private static final String[] HATE_SPEECH_KEYWORDS = {
            "discriminat", "prejudic", "racist", "sexist", "homophobic", "transphobic",
            "xenophobic", "antisemit", "islamophob", "ableist"
    };

    // Bias detection keywords
    private static final String[] BIAS_KEYWORDS = {
            "women aren't good at", "men aren't good at", "all [gender] are",
            "typical [race]", "you people", "illegal alien", "anchor baby",
            "ghetto", "thug", "welfare queen"
    };

    // Profanity patterns (basic)
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
            "\\b(damn|hell|crap)\\b", Pattern.CASE_INSENSITIVE
    );

    // Inappropriate personal judgments
    private static final String[] INAPPROPRIATE_JUDGMENTS = {
            "ugly", "fat", "stupid", "idiot", "retard", "crazy", "insane",
            "psycho", "loser", "trash", "worthless", "garbage"
    };

    // Stereotyping patterns
    private static final String[] STEREOTYPE_KEYWORDS = {
            "naturally gifted", "naturally talented", "inherently", "genetically",
            "by nature", "typical behavior", "personality type"
    };

    private static final int MIN_WORD_LENGTH = 1;
    private static final int MAX_WORD_LENGTH = 150;

    public static ValidationResult validateOutput(String output) {
        ValidationResult result = new ValidationResult();

        if (output == null || output.isEmpty()) {
            result.setValid(false);
            result.addError("Output cannot be empty");
            return result;
        }

        // Perform all validations
        validateLength(output, result);
        validateHateSpeech(output, result);
        validateBias(output, result);
        validateProfanity(output, result);
        validateInappropriateJudgments(output, result);
        validateStereotyping(output, result);
        validateCoherence(output, result);

        return result;
    }

    private static void validateLength(String output, ValidationResult result) {
        String[] words = output.split("\\s+");

        for (String word : words) {
            if (word.length() > MAX_WORD_LENGTH) {
                result.addWarning("Unusually long word detected: " + word.substring(0, 50) + "...");
            }
            if (word.length() < MIN_WORD_LENGTH && !word.isEmpty()) {
                result.addWarning("Invalid word format detected");
            }
        }

        if (words.length < 5) {
            result.addWarning("Output seems too short to be meaningful");
        }
    }

    private static void validateHateSpeech(String output, ValidationResult result) {
        String lowerOutput = output.toLowerCase();

        for (String keyword : HATE_SPEECH_KEYWORDS) {
            if (lowerOutput.contains(keyword)) {
                result.setValid(false);
                result.addError("Potential hate speech detected: " + keyword);
            }
        }
    }

    private static void validateBias(String output, ValidationResult result) {
        String lowerOutput = output.toLowerCase();

        for (String biasKeyword : BIAS_KEYWORDS) {
            if (lowerOutput.contains(biasKeyword.toLowerCase())) {
                result.setValid(false);
                result.addError("Potential bias detected: " + biasKeyword);
            }
        }
    }

    private static void validateProfanity(String output, ValidationResult result) {
        if (PROFANITY_PATTERN.matcher(output).find()) {
            result.addWarning("Mild profanity detected in output");
        }
    }

    private static void validateInappropriateJudgments(String output, ValidationResult result) {
        String lowerOutput = output.toLowerCase();

        for (String judgment : INAPPROPRIATE_JUDGMENTS) {
            if (lowerOutput.contains(judgment)) {
                result.setValid(false);
                result.addError("Inappropriate personal judgment detected: " + judgment);
            }
        }
    }

    private static void validateStereotyping(String output, ValidationResult result) {
        String lowerOutput = output.toLowerCase();

        for (String stereotype : STEREOTYPE_KEYWORDS) {
            if (lowerOutput.contains(stereotype)) {
                result.addWarning("Potential stereotyping language detected: " + stereotype);
            }
        }
    }

    private static void validateCoherence(String output, ValidationResult result) {
        // Check for repeated phrases (potential AI malfunction)
        String[] sentences = output.split("\\. ");
        Set<String> uniqueSentences = new HashSet<>(Arrays.asList(sentences));

        if (sentences.length > 3 && (double) uniqueSentences.size() / sentences.length < 0.5) {
            result.addWarning("Output contains too many repeated phrases - may be low quality");
        }

        // Check for common signs of poor quality output
        if (output.contains("[INVALID]") || output.contains("[ERROR]") || output.contains("[NULL]")) {
            result.addWarning("Output contains error markers");
        }
    }

    public static String sanitizeOutput(String output) {
        if (output == null) {
            return "";
        }

        String sanitized = output;

        // Remove any personal information that might have leaked through
        sanitized = sanitized.replaceAll(
                "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
                "[EMAIL]"
        );

        // Remove phone numbers
        sanitized = sanitized.replaceAll(
                "\\b(?:\\d{3}[-.]?){2}\\d{4}\\b",
                "[PHONE]"
        );

        // Remove URLs
        sanitized = sanitized.replaceAll(
                "(?:https?://|www\\.)[\\w./?=-]+",
                "[URL]"
        );

        return sanitized.trim();
    }

    /**
     * Validation result class to hold validation status and messages
     */
    public static class ValidationResult {
        private boolean valid = true;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getSummary() {
            StringBuilder summary = new StringBuilder();

            if (!errors.isEmpty()) {
                summary.append("ERRORS (").append(errors.size()).append("): ");
                summary.append(String.join(", ", errors));
            }

            if (!warnings.isEmpty()) {
                if (summary.length() > 0) {
                    summary.append(" | ");
                }
                summary.append("WARNINGS (").append(warnings.size()).append("): ");
                summary.append(String.join(", ", warnings));
            }

            if (errors.isEmpty() && warnings.isEmpty()) {
                summary.append("✅ Output passed all validation checks");
            }

            return summary.toString();
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors +
                    ", warnings=" + warnings +
                    '}';
        }
    }
}
