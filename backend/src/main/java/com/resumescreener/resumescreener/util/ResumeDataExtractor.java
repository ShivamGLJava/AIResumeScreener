package com.resumescreener.resumescreener.util;

import java.util.*;
import java.util.regex.Pattern;

public class ResumeDataExtractor {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([0-9]{3})\\s*\\)|([0-9]{3}))\\s*(?:[.-]\\s*)?([0-9]{3})\\s*(?:[.-]\\s*)?([0-9]{4})|(?:\\+?[0-9]{1,3}\\s*)?[0-9]{7,}(?:\\s*(?:#|x|ext\\.?)\\s*[0-9]{3,})?)");

    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("(?i)(\\b(?:\\d{1,5}\\s+)?(?:north|south|east|west|n|s|e|w)?\\s*(?:[a-z]+\\s+)?(?:street|st|avenue|ave|road|rd|boulevard|blvd|drive|dr|court|ct|lane|ln|square|sq|trail|trl|parkway|pkwy|circle|cir|plaza|plz|mount|mt|mountain|mtn)\\b.*?(?:city|town|state|zip|postal|code|province)\\s*[a-z]{2}\\s*[0-9]{5}|(?:[A-Z]{2}\\s+[0-9]{5}(?:-[0-9]{4})?))", Pattern.MULTILINE);

    private static final Pattern LINKEDIN_PATTERN =
            Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w-]+");

    private static final Pattern GITHUB_PATTERN =
            Pattern.compile("(?:https?://)?(?:www\\.)?github\\.com/[\\w-]+");

    private static final Pattern WEBSITE_PATTERN =
            Pattern.compile("(?:https?://)?(?:www\\.)?[\\w-]+\\.[a-z]{2,}");

    private static final String[] PII_KEYWORDS = {
            "ssn", "social security", "date of birth", "dob", "passport",
            "driver's license", "credit card", "bank account", "salary",
            "emergency contact", "mother's maiden"
    };

    public static String extractCleanResumeData(String resumeText) {
        if (resumeText == null || resumeText.isEmpty()) {
            return "";
        }

        String cleanedText = resumeText;

        // Remove personal information
        cleanedText = removeEmails(cleanedText);
        cleanedText = removePhoneNumbers(cleanedText);
        cleanedText = removeAddresses(cleanedText);
        cleanedText = removeSocialMediaLinks(cleanedText);
        cleanedText = removePIIKeywords(cleanedText);

        return cleanedText;
    }

    private static String removeEmails(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REMOVED]");
    }

    private static String removePhoneNumbers(String text) {
        return PHONE_PATTERN.matcher(text).replaceAll("[PHONE_REMOVED]");
    }

    private static String removeAddresses(String text) {
        return ADDRESS_PATTERN.matcher(text).replaceAll("[ADDRESS_REMOVED]");
    }

    private static String removeSocialMediaLinks(String text) {
        String cleaned = LINKEDIN_PATTERN.matcher(text).replaceAll("[LINKEDIN_REMOVED]");
        cleaned = GITHUB_PATTERN.matcher(cleaned).replaceAll("[GITHUB_REMOVED]");
        cleaned = WEBSITE_PATTERN.matcher(cleaned).replaceAll("[WEBSITE_REMOVED]");
        return cleaned;
    }

    private static String removePIIKeywords(String text) {
        String lowerText = text.toLowerCase();
        for (String keyword : PII_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                String pattern = "(?i).*" + Pattern.quote(keyword) + ".*";
                text = text.replaceAll(pattern, "[" + keyword.toUpperCase() + "_REMOVED]");
            }
        }
        return text;
    }

    public static Map<String, Object> extractRelevantData(String cleanedResumeText) {
        Map<String, Object> relevantData = new LinkedHashMap<>();

        relevantData.put("skills", extractSkills(cleanedResumeText));
        relevantData.put("experience", extractExperience(cleanedResumeText));
        relevantData.put("education", extractEducation(cleanedResumeText));
        relevantData.put("projects", extractProjects(cleanedResumeText));
        relevantData.put("certifications", extractCertifications(cleanedResumeText));
        relevantData.put("technologies", extractTechnologies(cleanedResumeText));

        return relevantData;
    }

    private static List<String> extractSkills(String text) {
        List<String> skills = new ArrayList<>();
        String lowerText = text.toLowerCase();

        String[] skillKeywords = {
                "java", "python", "javascript", "typescript", "c++", "c#", "go", "rust",
                "spring", "spring boot", "hibernate", "jpa", "maven", "gradle",
                "react", "angular", "vue", "node.js", "express",
                "sql", "nosql", "mongodb", "postgresql", "mysql", "redis",
                "docker", "kubernetes", "ci/cd", "jenkins", "gitlab",
                "aws", "azure", "gcp", "cloud", "microservices",
                "rest api", "graphql", "soap", "json", "xml",
                "agile", "scrum", "kanban", "devops", "git",
                "html", "css", "bootstrap", "tailwind",
                "testing", "junit", "pytest", "cypress", "selenium"
        };

        for (String skill : skillKeywords) {
            if (lowerText.contains(skill)) {
                skills.add(skill);
            }
        }

        return skills;
    }

    private static String extractExperience(String text) {
        StringBuilder experience = new StringBuilder();

        String[] experienceKeywords = {"experience", "worked", "responsible", "managed", "led", "developed", "designed", "implemented"};

        String[] lines = text.split("\n");
        boolean inExperienceSection = false;

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            if (lowerLine.contains("experience") || lowerLine.contains("professional history")) {
                inExperienceSection = true;
                continue;
            }

            if (inExperienceSection) {
                if (lowerLine.contains("education") || lowerLine.contains("skills") || lowerLine.contains("projects")) {
                    break;
                }

                for (String keyword : experienceKeywords) {
                    if (lowerLine.contains(keyword)) {
                        experience.append(line).append("\n");
                        break;
                    }
                }
            }
        }

        return experience.toString().trim();
    }

    private static List<String> extractEducation(String text) {
        List<String> education = new ArrayList<>();
        String lowerText = text.toLowerCase();

        String[] degreeKeywords = {"bachelor", "master", "phd", "diploma", "associate", "bs", "ms", "ba", "ma"};

        String[] lines = text.split("\n");
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            for (String degree : degreeKeywords) {
                if (lowerLine.contains(degree)) {
                    education.add(line.trim());
                    break;
                }
            }
        }

        return education;
    }

    private static List<String> extractProjects(String text) {
        List<String> projects = new ArrayList<>();

        String[] lines = text.split("\n");
        boolean inProjectsSection = false;

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            if (lowerLine.contains("projects") || lowerLine.contains("portfolio")) {
                inProjectsSection = true;
                continue;
            }

            if (inProjectsSection) {
                if (lowerLine.contains("experience") || lowerLine.contains("skills") || lowerLine.contains("education")) {
                    break;
                }

                if (!line.trim().isEmpty() && !lowerLine.contains("projects")) {
                    projects.add(line.trim());
                }
            }
        }

        return projects;
    }

    private static List<String> extractCertifications(String text) {
        List<String> certifications = new ArrayList<>();
        String lowerText = text.toLowerCase();

        String[] certKeywords = {"certified", "certification", "aws certified", "oracle certified", "microsoft certified", "google cloud", "certificate"};

        String[] lines = text.split("\n");
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            for (String cert : certKeywords) {
                if (lowerLine.contains(cert)) {
                    certifications.add(line.trim());
                    break;
                }
            }
        }

        return certifications;
    }

    private static List<String> extractTechnologies(String text) {
        List<String> technologies = new ArrayList<>();
        String lowerText = text.toLowerCase();

        String[] techKeywords = {
                "java", "python", "javascript", "typescript",
                "spring", "hibernate", "react", "angular",
                "mongodb", "postgresql", "mysql",
                "docker", "kubernetes", "jenkins",
                "aws", "azure", "gcp",
                "linux", "windows", "macos"
        };

        for (String tech : techKeywords) {
            if (lowerText.contains(tech)) {
                technologies.add(tech);
            }
        }

        return new ArrayList<>(new HashSet<>(technologies)); // Remove duplicates
    }
}
