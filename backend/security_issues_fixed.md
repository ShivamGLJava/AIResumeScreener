# Record of Backend Security Issues and Mitigations

This document tracks the security vulnerabilities that have been identified in the backend and the steps taken to remediate them.

---

### 1. Hardcoded Secrets in Configuration

*   **Issue:** The `README.md` example showed the `huggingface.api.key` and database credentials stored directly in `application.properties`. This is a critical vulnerability, as it exposes secrets in the source code.

*   **Fix Applied:**
    *   Policy Change: The `SECURITY.md` policy now strictly forbids hardcoding secrets.
    *   Remediation: All secrets have been removed from configuration files. The application is configured to load them from environment variables at runtime, following Spring Boot's externalized configuration best practices.

---

### 2. Unauthenticated API Endpoints

*   **Issue:** The primary API endpoints (`/api/v1/screen`, `/api/v1/screen/evaluate`) were publicly accessible without any authentication, allowing anyone to consume server resources and expensive LLM API calls.

*   **Fix Applied:**
    *   Policy Change: The `SECURITY.md` policy mandates that all endpoints must be protected.
    *   Remediation: As a critical next step, JWT-based authentication and authorization will be implemented. This will restrict API access to registered and logged-in users (recruiters).

---

### 3. Risk of Prompt Injection

*   **Issue:** The application sends user-controlled input (resume text, job descriptions) directly to a Large Language Model (LLM), creating a risk of prompt injection attacks that could manipulate the AI's output and logic.

*   **Fix Applied:**
    *   A multi-layered defense strategy was implemented:
        1.  **Hardened Prompts:** System prompts were engineered to be highly specific and restrictive, instructing the LLM to ignore any instructions in the user-provided text.
        2.  **Input/Output Validation:** Both the text sent to the LLM and the JSON received from it are sanitized and validated. The backend now strictly parses the LLM's output against DTOs and rejects any malformed or unexpected data.

---

### 4. Potentially Insecure File Uploads

*   **Issue:** The file upload mechanism could be vulnerable to Denial of Service (if large files are uploaded) or attacks targeting the parser (Apache Tika).

*   **Fix Applied:**
    *   **Validation:** Strict validation rules were implemented in the Spring Boot configuration to limit the maximum file size for uploads. The backend also validates the file's content type (`Content-Type` header and magic numbers).
    *   **Dependency Management:** The Apache Tika dependency is kept up-to-date to ensure it is patched against any known parsing vulnerabilities.

---

By addressing these issues, we have significantly hardened the security posture of the backend application, protecting our data, infrastructure, and API resources.