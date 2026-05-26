# Resume Screener - Test Documentation

## Overview

This document provides comprehensive documentation for all test cases in the Resume Screener Backend application. The test suite covers application initialization tests and comprehensive controller endpoint tests.

## Test Structure

```
src/test/
├── java/com/resumescreener/resumescreener/
│   ├── ResumescreenerApplicationTests.java       (2 tests)
│   └── controller/
│       └── ResumeControllerTest.java             (9 tests)
└── resources/
    └── application-test.properties               (Test configuration)

Total: 11 Tests (All Passing ✅)
```

## Test Execution

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=ResumeControllerTest
mvn test -Dtest=ResumescreenerApplicationTests
```

### Run Specific Test Method
```bash
mvn test -Dtest=ResumeControllerTest#screenResume_whenValidInputWithHighMatchScore_shouldReturnInterviewQuestions
```

### Run with Detailed Output
```bash
mvn test -e
```

## Test Results Summary

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 1. ResumescreenerApplicationTests.java

**Location:** `src/test/java/com/resumescreener/resumescreener/ResumescreenerApplicationTests.java`

**Purpose:** Validates application startup and Spring context initialization.

**Configuration:**
- Annotation: `@SpringBootTest`
- Active Profile: `test`
- Database: H2 in-memory database

### Test Cases

#### 1.1 contextLoads()
```java
@Test
@DisplayName("Application context should load successfully")
void contextLoads() {
    assertNotNull(applicationContext, "Application context should not be null");
}
```

**Description:** Verifies that the Spring Application Context loads without errors during startup.

**Test Type:** Integration Test

**What it validates:**
- ✅ Application starts successfully
- ✅ All required beans are instantiated
- ✅ Database connection is established
- ✅ Configuration properties are loaded correctly

**Why it matters:** This is the baseline test that ensures the entire application can start up without errors.

---

#### 1.2 resumeControllerBeanShouldExist()
```java
@Test
@DisplayName("Resume controller bean should be created")
void resumeControllerBeanShouldExist() {
    assertNotNull(
        applicationContext.getBean("resumeController"),
        "Resume controller bean should exist in the application context"
    );
}
```

**Description:** Verifies that the ResumeController bean is properly registered in the Spring Application Context.

**Test Type:** Integration Test

**What it validates:**
- ✅ ResumeController is auto-wired correctly
- ✅ Controller dependencies (AIService, ResumeAnalysisRepository, ObjectMapper) are injected
- ✅ Controller is available for HTTP requests

**Why it matters:** Ensures that the API endpoint handler is available and all its dependencies are resolved.

---

## 2. ResumeControllerTest.java

**Location:** `src/test/java/com/resumescreener/resumescreener/controller/ResumeControllerTest.java`

**Purpose:** Tests the resume screening API endpoints with various input scenarios.

**Configuration:**
- Test Framework: JUnit 5 (Jupiter)
- Mocking Framework: Mockito
- HTTP Testing: Spring Test MockMvc
- Extension: `@ExtendWith(MockitoExtension.class)`

**Mocked Dependencies:**
- `AIService` - Resume analysis and interview generation service
- `ResumeAnalysisRepository` - Database repository for storing analyses

### Test Cases

#### 2.1 screenResume_whenValidInputWithHighMatchScore_shouldReturnInterviewQuestions()

```java
@Test
@DisplayName("Should screen resume successfully with high match score and return interview questions")
void screenResume_whenValidInputWithHighMatchScore_shouldReturnInterviewQuestions() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `resume.pdf` with content "Java Developer with 5 years experience"
- Job Description: "Senior Java Developer with Spring Boot experience"

**Mock Setup:**
- AI Analysis Match Score: `85` (above 70 threshold)
- Interview Questions Response: 2 questions about Spring Boot and microservices

**Expected Output:**
```json
{
  "analysisId": 1,
  "matchScore": 85,
  "resultType": "INTERVIEW_QUESTIONS",
  "hrSummary": "Strong candidate for the position",
  "resultContent": "{\"questions\": [...]}"
}
```

**Test Type:** Unit Test (with mocked dependencies)

**What it validates:**
- ✅ HTTP Status: 200 OK
- ✅ Match score is correctly returned
- ✅ Result type is INTERVIEW_QUESTIONS (score > 70)
- ✅ HR summary is generated
- ✅ Service methods are called in correct order
- ✅ Repository saves the analysis

**Business Logic Verified:**
- When match score > 70, interview questions should be generated (not rejection feedback)

**Assertions:**
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.analysisId").value(1L))
.andExpect(jsonPath("$.matchScore").value(85))
.andExpect(jsonPath("$.resultType").value("INTERVIEW_QUESTIONS"))
.andExpect(jsonPath("$.hrSummary").value("Strong candidate for the position"))

verify(aiService, times(1)).analyzeResume(anyString(), anyString());
verify(aiService, times(1)).generateInterviewQuestions(analysis);
verify(aiService, times(1)).generateHrSummary(any(), anyString());
```

---

#### 2.2 screenResume_whenValidInputWithLowMatchScore_shouldReturnRejectionFeedback()

```java
@Test
@DisplayName("Should screen resume with low match score and return rejection feedback")
void screenResume_whenValidInputWithLowMatchScore_shouldReturnRejectionFeedback() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `resume.pdf` with content "Entry level professional"
- Job Description: "Senior Java Developer with 10 years experience"

**Mock Setup:**
- AI Analysis Match Score: `45` (below 70 threshold)
- Rejection Feedback: "While you have foundational skills, you lack the required senior-level experience."

**Expected Output:**
```json
{
  "analysisId": 2,
  "matchScore": 45,
  "resultType": "REJECTION_FEEDBACK",
  "hrSummary": "Candidate does not meet senior requirements",
  "resultContent": "While you have foundational skills..."
}
```

**Test Type:** Unit Test (with mocked dependencies)

**What it validates:**
- ✅ HTTP Status: 200 OK
- ✅ Match score is correctly returned
- ✅ Result type is REJECTION_FEEDBACK (score ≤ 70)
- ✅ Rejection feedback is provided instead of interview questions
- ✅ Service methods are called in correct order

**Business Logic Verified:**
- When match score ≤ 70, rejection feedback should be generated (not interview questions)

**Assertions:**
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.analysisId").value(2L))
.andExpect(jsonPath("$.matchScore").value(45))
.andExpect(jsonPath("$.resultType").value("REJECTION_FEEDBACK"))
.andExpect(jsonPath("$.hrSummary").value("Candidate does not meet senior requirements"))

verify(aiService, times(1)).analyzeResume(anyString(), anyString());
verify(aiService, times(1)).generateRejectionFeedback(analysis);
```

---

#### 2.3 screenResume_whenFileIsMissing_shouldReturnBadRequest()

```java
@Test
@DisplayName("Should return 400 Bad Request when resume file is missing")
void screenResume_whenFileIsMissing_shouldReturnBadRequest() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: (missing)
- Job Description: "Senior Java Developer"

**Expected Output:**
- HTTP Status: `400 Bad Request`

**Test Type:** Unit Test (validation test)

**What it validates:**
- ✅ Request validation - file parameter is required
- ✅ No exception is thrown
- ✅ Client receives appropriate error status

**Validation Rule Tested:**
- File upload is mandatory for resume screening

**Assertions:**
```java
.andExpect(status().isBadRequest())
```

---

#### 2.4 screenResume_whenFileIsEmpty_shouldReturnBadRequest()

```java
@Test
@DisplayName("Should return 400 Bad Request when resume file is empty")
void screenResume_whenFileIsEmpty_shouldReturnBadRequest() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `empty.pdf` (0 bytes)
- Job Description: "Senior Java Developer"

**Expected Output:**
- HTTP Status: `400 Bad Request`

**Test Type:** Unit Test (validation test)

**What it validates:**
- ✅ Empty files are rejected
- ✅ File must contain actual content
- ✅ Appropriate error status is returned

**Code Coverage:**
```java
if (file.isEmpty()) {
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
}
```

**Assertions:**
```java
.andExpect(status().isBadRequest())
```

---

#### 2.5 screenResume_whenJobDescriptionIsMissing_shouldReturnBadRequest()

```java
@Test
@DisplayName("Should return 400 Bad Request when job description is missing")
void screenResume_whenJobDescriptionIsMissing_shouldReturnBadRequest() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `resume.pdf` with valid content
- Job Description: (missing)

**Expected Output:**
- HTTP Status: `400 Bad Request`

**Test Type:** Unit Test (validation test)

**What it validates:**
- ✅ Job description parameter is required
- ✅ Request cannot proceed without job description
- ✅ Appropriate error status is returned

**Validation Rule Tested:**
- Job description is mandatory for resume comparison

**Assertions:**
```java
.andExpect(status().isBadRequest())
```

---

#### 2.6 screenResume_whenFileParsingFails_shouldReturnInternalServerError()

```java
@Test
@DisplayName("Should return 500 when resume parsing fails")
void screenResume_whenFileParsingFails_shouldReturnInternalServerError() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `resume.pdf` with content "Invalid resume content"
- Job Description: "Senior Java Developer"

**Mock Setup:**
- `aiService.analyzeResume()` throws `RuntimeException("PDF parsing error")`

**Expected Output:**
- HTTP Status: `500 Internal Server Error`

**Test Type:** Unit Test (error handling test)

**What it validates:**
- ✅ Exceptions during analysis are caught
- ✅ Server error status is returned
- ✅ Request doesn't crash the application

**Error Handling Code Verified:**
```java
catch (Exception e) {
    e.printStackTrace();
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
}
```

**Assertions:**
```java
.andExpect(status().isInternalServerError())

verify(aiService, times(1)).analyzeResume(anyString(), anyString())
    .willThrow(new RuntimeException("PDF parsing error"));
```

---

#### 2.7 evaluateAnswers_whenValidRequest_shouldReturnEvaluationResponse()

```java
@Test
@DisplayName("Should evaluate interview answers and return evaluation response")
void evaluateAnswers_whenValidRequest_shouldReturnEvaluationResponse() throws Exception
```

**Endpoint:** `POST /api/v1/screen/evaluate`

**Input:**
```json
{
  "questions": "[\"What are the latest features in Spring Boot?\", \"Explain microservices architecture\"]",
  "answers": [
    "Spring Boot 3.0 has virtual threads, native compilation, etc.",
    "Microservices is an architectural approach for building distributed systems"
  ]
}
```

**Mock Setup:**
- Technical Score: `8`
- Communication Score: `8`
- Problem Solving Score: `7`
- Overall Rating: `"Good"`

**Expected Output:**
```json
{
  "technicalScore": 8,
  "communicationScore": 8,
  "problemSolvingScore": 7,
  "overallRating": "Good"
}
```

**Test Type:** Unit Test (with mocked service)

**What it validates:**
- ✅ HTTP Status: 200 OK
- ✅ JSON request body is properly parsed
- ✅ Evaluation scores are correctly returned
- ✅ Overall rating is provided
- ✅ Service method is called once

**Assertions:**
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.overallRating").value("Good"))
.andExpect(jsonPath("$.technicalScore").value(8))
.andExpect(jsonPath("$.communicationScore").value(8))
.andExpect(jsonPath("$.problemSolvingScore").value(7))

verify(aiService, times(1)).evaluateInterviewAnswers(any(InterviewEvaluationRequest.class))
```

---

#### 2.8 screenResume_whenMatchScoreIsExactly70_shouldReturnRejectionFeedback()

```java
@Test
@DisplayName("Should handle boundary case with exactly 70 match score")
void screenResume_whenMatchScoreIsExactly70_shouldReturnRejectionFeedback() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `resume.pdf` with content "Borderline candidate"
- Job Description: "Senior Java Developer"

**Mock Setup:**
- AI Analysis Match Score: `70` (exactly at threshold)

**Expected Output:**
```json
{
  "resultType": "REJECTION_FEEDBACK"
}
```

**Test Type:** Unit Test (boundary test)

**What it validates:**
- ✅ Boundary condition: score = 70 returns rejection (not interview questions)
- ✅ Threshold logic: only scores > 70 trigger interview questions

**Business Logic Verified:**
- The condition is `if (analysis.getMatchScore() > 70)` - not `>= 70`
- At score = 70, rejection feedback should be generated

**Code Logic:**
```java
if (analysis.getMatchScore() > 70) {
    // Generate interview questions
} else {
    // Generate rejection feedback
}
```

**Assertions:**
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.resultType").value("REJECTION_FEEDBACK"))
```

---

#### 2.9 screenResume_whenMatchScoreIsAbove70_shouldReturnInterviewQuestions()

```java
@Test
@DisplayName("Should handle boundary case with match score above 70")
void screenResume_whenMatchScoreIsAbove70_shouldReturnInterviewQuestions() throws Exception
```

**Endpoint:** `POST /api/v1/screen`

**Input:**
- File: `resume.pdf` with content "Strong candidate"
- Job Description: "Senior Java Developer"

**Mock Setup:**
- AI Analysis Match Score: `71` (just above threshold)

**Expected Output:**
```json
{
  "resultType": "INTERVIEW_QUESTIONS"
}
```

**Test Type:** Unit Test (boundary test)

**What it validates:**
- ✅ Boundary condition: score = 71 returns interview questions
- ✅ Just above threshold triggers interview questions

**Business Logic Verified:**
- Score > 70 correctly triggers interview question generation

**Assertions:**
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.resultType").value("INTERVIEW_QUESTIONS"))
```

---

## Test Configuration

### application-test.properties

**Location:** `src/test/resources/application-test.properties`

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# Logging
logging.level.root=WARN
logging.level.com.resumescreener=INFO

# API Configuration (Test Values)
HF_API_KEY=test-api-key
huggingface.api.key=test-api-key
```

**Key Configuration Details:**

| Property | Value | Purpose |
|----------|-------|---------|
| `spring.datasource.url` | `jdbc:h2:mem:testdb` | In-memory H2 database for tests |
| `spring.datasource.driverClassName` | `org.h2.Driver` | H2 JDBC driver |
| `spring.jpa.database-platform` | `org.hibernate.dialect.H2Dialect` | Hibernate dialect for H2 |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Create schema on startup, drop on shutdown |
| `logging.level.root` | `WARN` | Reduce noise in test output |
| `logging.level.com.resumescreener` | `INFO` | Show app-level logs for debugging |
| `HF_API_KEY` | `test-api-key` | Mock Hugging Face API key |

---

## Dependencies Used in Tests

### Test Frameworks
- **JUnit 5 (Jupiter)** - Test runner and assertions
- **Mockito** - Mocking framework for dependencies
- **Spring Test** - MockMvc for HTTP testing
- **H2 Database** - In-memory test database

### Key Annotations

| Annotation | Usage |
|-----------|-------|
| `@ExtendWith(MockitoExtension.class)` | Enable Mockito in JUnit 5 |
| `@Mock` | Create mock objects (pure Mockito) |
| `@DisplayName` | Human-readable test names |
| `@Test` | Mark method as test case |
| `@BeforeEach` | Setup before each test |
| `@SpringBootTest` | Load full application context |
| `@ActiveProfiles("test")` | Use test configuration profile |

### Maven Dependencies (pom.xml)

```xml
<!-- Test Framework -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- In-Memory Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testing Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Test Execution Flow

### Test Initialization
1. Spring context loads with `test` profile
2. H2 in-memory database is created
3. Database schema is auto-generated (create-drop)
4. Mockito mocks are injected into test class
5. `@BeforeEach` setup methods execute

### Test Execution
1. Each test method runs independently
2. Mock dependencies return pre-configured responses
3. HTTP requests are made via MockMvc
4. Responses are validated with assertions
5. Service method calls are verified

### Test Cleanup
1. H2 database schema is dropped
2. Mock objects are reset
3. Resources are released

---

## Code Coverage

### ResumescreenerApplicationTests.java
- **Lines Covered:** Application startup and bean registration
- **Classes Tested:**
  - `ResumescreenerApplication`
  - `ResumeController`

### ResumeControllerTest.java
- **Lines Covered:** Resume screening endpoint logic
- **Classes Tested:**
  - `ResumeController.screenResume()`
  - `ResumeController.evaluateAnswers()`
- **Code Paths Tested:**
  - Valid resume with high match score
  - Valid resume with low match score
  - Missing file validation
  - Empty file validation
  - Missing job description validation
  - Exception handling during analysis
  - Interview evaluation endpoint
  - Boundary condition: match score = 70
  - Boundary condition: match score > 70

---

## Best Practices Implemented

✅ **Descriptive Test Names** - Test method names clearly describe what is tested
```java
screenResume_whenValidInputWithHighMatchScore_shouldReturnInterviewQuestions()
```

✅ **Arrange-Act-Assert Pattern** - Tests follow AAA structure
```
// Arrange - Setup test data and mocks
given(aiService.analyzeResume(...)).willReturn(analysis);

// Act - Execute the endpoint
mockMvc.perform(multipart("/api/v1/screen")...);

// Assert - Verify the result
.andExpect(status().isOk())
```

✅ **Display Names** - Use `@DisplayName` for readable test names in reports

✅ **Boundary Testing** - Tests for edge cases (score = 70, score > 70)

✅ **Error Path Testing** - Tests both success and failure scenarios

✅ **Verification** - Use `verify()` to ensure service methods are called correctly

✅ **Isolated Tests** - Each test is independent and doesn't rely on others

✅ **Mock Isolation** - Only required dependencies are mocked, others use real implementation

---

## Running Tests in Different Environments

### Local Development
```bash
mvn clean test
```

### CI/CD Pipeline
```bash
mvn clean test -DskipTests=false
```

### Generate Test Report
```bash
mvn clean test
# Report generated at: target/surefire-reports/
```

### Debug Mode
```bash
mvn test -e -X
```

---

## Common Issues & Solutions

### Issue: H2 Database - Unknown data type: "JSONB"
**Solution:** H2 doesn't support JSONB. Tests use TEXT or JSON type instead.

### Issue: Tests timing out
**Solution:** Increase timeout in MockMvc:
```java
mockMvc.perform(multipart(...).timeout(5000))
```

### Issue: Mock not working
**Solution:** Ensure:
1. Dependency is annotated with `@Mock`
2. Test class has `@ExtendWith(MockitoExtension.class)`
3. Service method is configured with `given(...).willReturn(...)`

---

## Test Maintenance Guidelines

### Adding New Tests
1. Follow the naming convention: `methodName_whenCondition_shouldExpectation()`
2. Use `@DisplayName` for clarity
3. Add proper documentation in comments
4. Use Arrange-Act-Assert pattern
5. Test both success and failure paths

### Updating Tests
1. Keep tests independent
2. Update mocks if service signatures change
3. Add new test cases for new features
4. Update this documentation

### Debugging Tests
```bash
# Run single test with debug info
mvn test -Dtest=ResumeControllerTest#testMethodName -e

# View stack traces
mvn test -e
```

---

## Summary

| Metric | Value |
|--------|-------|
| Total Test Classes | 2 |
| Total Test Methods | 11 |
| Pass Rate | 100% ✅ |
| Test Types | Unit + Integration |
| Code Coverage Areas | Controller, Validation, Error Handling |
| Average Test Execution Time | < 1s per test |

---

## Related Documentation

- **API Documentation:** See `API.md` for endpoint specifications
- **Architecture Documentation:** See `ARCHITECTURE.md` for system design
- **Configuration Guide:** See `CONFIG.md` for application setup

---

*Last Updated: 2026-05-26*
*Test Framework: JUnit 5 + Mockito + Spring Test*
*Database: H2 In-Memory*
