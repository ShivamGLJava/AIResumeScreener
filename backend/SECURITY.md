# Backend Security Policy

This document outlines security procedures and policies for the Spring Boot backend of the AI Resume Screener project.

## Reporting a Vulnerability

We take all security vulnerabilities seriously. If you discover a security issue, please email the project maintainers with a detailed report, including steps to reproduce the vulnerability. We will acknowledge your report within 48 hours. We ask that you do not disclose the issue publicly until we have had a chance to address it.

## Secure Development Practices

The following practices are enforced to maintain the security and integrity of the backend application.

### 1. Dependency Management

*   **Regular Audits:** We regularly audit our Maven dependencies for known vulnerabilities using tools like the OWASP Dependency-Check plugin or commercial equivalents like Snyk.
*   **Update Policy:** Critical and high-severity vulnerabilities in dependencies, such as those in Apache Tika or Jackson, are addressed immediately by updating to a patched version.

### 2. Secrets Management

*   **No Hardcoded Secrets:** API keys (e.g., `huggingface.api.key`) and database credentials must not be hardcoded in `application.properties` or committed to version control.
*   **Mitigation:** All secrets are externalized using environment variables or a dedicated secrets management service (like HashiCorp Vault or AWS Secrets Manager). Spring Boot's externalized configuration feature is used to load these values at runtime.

### 3. API Security

*   **Authentication & Authorization:** All API endpoints are protected. As outlined in the project's future enhancements, JWT-based authentication will be implemented to ensure that only authenticated and authorized users (e.g., recruiters) can access the screening and evaluation APIs. Unauthenticated access is a significant security risk and is not permitted.
*   **CORS Configuration:** The Cross-Origin Resource Sharing (CORS) policy is configured to be restrictive. The `@CrossOrigin` annotation or `CorsConfig.java` is configured to only allow requests from the specific, trusted domain of the frontend application, rather than using a wildcard (`*`).

### 4. Input Validation

All incoming data is treated as untrusted and is rigorously validated.

*   **File Uploads:**
    *   **Type and Size:** Uploaded resumes are validated to ensure they match allowed file types (PDF, DOCX) and do not exceed a reasonable size limit to prevent Denial of Service (DoS) attacks.
    *   **Content Parsing:** We use an up-to-date version of Apache Tika for parsing. The extracted text is sanitized before further processing.

*   **Prompt Injection:**
    *   **Vulnerability:** User-provided content (from resumes and job descriptions) sent to the LLM poses a risk of prompt injection, where an attacker could manipulate the AI's behavior.
    *   **Mitigation:** We employ multiple layers of defense:
        1.  **Strict Prompts:** The system prompts sent to the LLM include strong, explicit instructions to only perform the requested task and to produce only valid, structured JSON, as mentioned in the project's `README.md`.
        2.  **Sanitization:** Input from users is sanitized to remove or escape characters that could be interpreted as instructions by the LLM.
        3.  **Output Validation:** The JSON response from the LLM is treated as untrusted input. It is parsed and validated against a strict DTO structure before being used by the application.

### 5. Protection Against Insecure Deserialization

*   **Vulnerability:** The Jackson `ObjectMapper` can be vulnerable to remote code execution if it deserializes untrusted JSON into arbitrary Java types (polymorphic typing).
*   **Mitigation:** We avoid enabling Jackson's "default typing" (`enableDefaultTyping`). All JSON is deserialized into simple, known Data Transfer Objects (DTOs) with a well-defined structure.

### 6. Secure Error Handling

*   **Global Exception Handling:** A global exception handler (`@ControllerAdvice`) is implemented to catch all unhandled exceptions.
*   **No Information Leakage:** In a production environment, this handler prevents sensitive information like stack traces from being sent to the client. It logs the detailed error internally and returns a generic, non-informative error message to the user.