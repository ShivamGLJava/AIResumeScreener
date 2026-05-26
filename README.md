# AI Resume Screener & Interview Evaluation System

## Overview

The **AI Resume Screener & Interview Evaluation System** is an intelligent full-stack application designed to automate the recruitment screening workflow using Large Language Models (LLMs).

The platform performs:

* Resume parsing
* AI-powered resume analysis
* Match score calculation
* Technical interview question generation
* Candidate answer evaluation
* HR summary generation
* Final hiring recommendation

The system uses:

* **Spring Boot** for backend APIs
* **React.js** for frontend UI
* **PostgreSQL** for persistence
* **Hugging Face Inference Router APIs**
* **Qwen/Qwen2.5-7B-Instruct LLM**
* **Apache Tika** for resume parsing

---

# Problem Statement

Traditional resume screening is:

* manual
* time-consuming
* inconsistent
* difficult to scale

Recruiters often spend:

* large amounts of time filtering resumes
* preparing interview questions
* evaluating candidates manually

This project automates the complete first-level technical screening process using Generative AI.

---

# Project Objectives

The main objectives were:

1. Automatically parse resumes
2. Compare resumes with job descriptions
3. Generate AI-based candidate analysis
4. Generate technical interview questions dynamically
5. Allow candidates to answer questions
6. Evaluate answers using LLMs
7. Generate HR summaries and recommendations
8. Store evaluation data in a database

---

# System Architecture

```text
Frontend (React.js)
        |
        v
Spring Boot REST APIs
        |
        v
AI Service Layer
        |
        v
Hugging Face Router API
        |
        v
Qwen/Qwen2.5-7B-Instruct
        |
        v
PostgreSQL Database
```

---

# Technologies Used

## Frontend

| Technology | Purpose        |
| ---------- | -------------- |
| React.js   | UI Development |
| Axios      | API Calls      |
| Bootstrap  | Styling        |
| HTML/CSS   | Layout         |
| JavaScript | Logic          |

---

## Backend

| Technology           | Purpose           |
| -------------------- | ----------------- |
| Spring Boot          | REST API backend  |
| Spring WebMVC        | Web APIs          |
| Spring WebFlux       | WebClient support |
| Spring Data JPA      | Database ORM      |
| Hibernate            | ORM               |
| PostgreSQL           | Database          |
| Apache Tika          | Resume parsing    |
| Jackson ObjectMapper | JSON parsing      |
| Reactor Mono         | Async AI calls    |

---

## AI/LLM

| Tool                     | Purpose                      |
| ------------------------ | ---------------------------- |
| Hugging Face Router API  | LLM Gateway                  |
| Qwen/Qwen2.5-7B-Instruct | Resume analysis & evaluation |

---

# Features

## 1. Resume Upload

Candidates upload:

* PDF resumes
* DOC/DOCX resumes

---

## 2. Resume Parsing

Apache Tika extracts:

* skills
* experience
* project details
* technologies

from uploaded resumes.

---

## 3. AI Resume Analysis

The LLM:

* compares resume with job description
* identifies strengths
* identifies weaknesses
* detects missing requirements
* calculates match score

---

## 4. AI Interview Question Generation

If candidate match score > threshold:

* AI generates technical interview questions dynamically

Questions are customized based on:

* skills
* projects
* technologies
* missing requirements

---

## 5. Candidate Answer Evaluation

Candidates answer generated questions.

LLM evaluates:

* technical understanding
* communication quality
* problem-solving ability

---

## 6. HR Summary Generation

System generates:

* evaluator summary
* strengths
* weaknesses
* recommendation

---

## 7. Database Persistence

All evaluations are stored in PostgreSQL.

---

# Development Journey

# Phase 1 — Initial Planning

Initially the project was planned as:

* Resume Upload System
* AI Resume Matcher

Later expanded into:

* complete interview evaluation platform

---

# Phase 2 — Backend Development

## Spring Boot Setup

Created:

* REST controllers
* service layer
* repository layer
* entity models

---

## Database Integration

Configured:

* PostgreSQL
* Spring Data JPA
* Hibernate

Created entity:

```java
ResumeAnalysis
```

to store:

* match score
* HR summary
* interview feedback

---

# Phase 3 — Resume Parsing

Integrated:

* Apache Tika

to support:

* PDF parsing
* DOCX parsing

Challenges:

* encoding issues
* special characters
* inconsistent extraction formatting

Solution:

* cleaned extracted text
* truncated logs
* validated extracted content

---

# Phase 4 — AI Integration

Initially attempted:

* DeepSeek models
* Llama models

Challenges:

* unavailable models
* API authorization failures
* unsupported model routing

Final stable model:

* Qwen/Qwen2.5-7B-Instruct

---

# Bugs Faced & Fixes

# Bug 1 — WebClient.Builder Bean Missing

## Error

```text
No qualifying bean of type WebClient.Builder
```

## Cause

Spring WebFlux dependency missing.

## Fix

Added:

```xml
spring-boot-starter-webflux
```

Created:

```java
WebClientConfig.java
```

---

# Bug 2 — ObjectMapper Bean Missing

## Error

```text
ObjectMapper bean could not be found
```

## Fix

Created:

```java
JacksonConfig.java
```

---

# Bug 3 — Hugging Face 401 Unauthorized

## Cause

* invalid token
* inaccessible model

## Fix

* generated valid HF token
* switched to Qwen model

---

# Bug 4 — JSON Parsing Failures

## Error

```text
Cannot deserialize String from Array value
```

## Cause

AI returned:

```json
"skills": []
```

but Java model expected:

```java
String skills;
```

## Fix

Updated DTOs to:

```java
List<String>
```

---

# Bug 5 — Invalid AI JSON

## Problem

LLM sometimes returned:

* incomplete JSON
* missing braces
* markdown

## Fixes

### Added:

* low temperature
* max_tokens
* strict prompts

### Added JSON sanitization:

```java
if (!responseJson.endsWith("}")) {
    responseJson += "}";
}
```

---

# Bug 6 — Frontend Parsing Issues

## Problem

Interview questions rendered incorrectly.

Cause:

* frontend split text by newline

## Fix

Changed backend to return structured JSON.

---

# Bug 7 — CORS Errors

## Error

```text
No Access-Control-Allow-Origin header
```

## Fix

Created:

```java
CorsConfig.java
```

---

# AI Prompt Engineering

Special focus was placed on:

* strict JSON responses
* deterministic outputs
* response consistency

Example:

```text
Return ONLY valid JSON.
Do not include markdown.
Do not include explanations.
```

---

# Backend APIs

## Resume Screening API

```http
POST /api/v1/screen
```

### Inputs

* resume file
* job description

### Outputs

* match score
* interview questions
* HR summary

---

## Candidate Evaluation API

```http
POST /api/v1/screen/evaluate
```

### Inputs

* questions
* candidate answers

### Outputs

* technical score
* recommendation
* evaluator summary

---

# Frontend Workflow

## Step 1

Upload resume.

## Step 2

Enter job description.

## Step 3

Backend analyzes resume.

## Step 4

AI generates interview questions.

## Step 5

Candidate answers questions.

## Step 6

Backend evaluates answers.

## Step 7

Evaluator receives final summary.

---

# Database Design

## ResumeAnalysis Entity

Stores:

* candidate data
* match score
* HR summary
* AI analysis
* evaluation results

---

# Security Considerations

Implemented:

* environment variables
* API key hiding
* CORS restrictions

---

# Performance Optimizations

Implemented:

* reusable WebClient
* reduced AI temperature
* optimized prompts
* structured DTO parsing

---

# Challenges Faced

## AI Reliability

LLMs often:

* hallucinated
* returned invalid JSON
* truncated responses

## Integration Complexity

Managing:

* React
* Spring Boot
* PostgreSQL
* Hugging Face
* Tika

simultaneously required extensive debugging.

## Prompt Engineering

Most difficult challenge:

* forcing reliable JSON responses from LLMs.

---

# Final Outcome

Successfully developed:

* AI resume screening system
* AI interview generation system
* AI candidate evaluation platform

with:

* dynamic workflows
* structured evaluation
* persistent storage
* end-to-end automation

---

# Future Enhancements

## Planned Improvements

### Authentication

* JWT authentication
* recruiter login

### Advanced AI

* multi-model evaluation
* RAG-based evaluation

### Analytics Dashboard

* recruiter dashboard
* hiring analytics

### Voice Interviews

* speech-to-text interview support

### Email Integration

* automated interview emails

### Docker Deployment

* containerized deployment

### Cloud Deployment

* AWS/GCP deployment

---

# How To Run

# Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

---

# Frontend

```bash
cd frontend
npm install
npm start
```

---

# Environment Variables

## application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/resumescreener
spring.datasource.username=postgres
spring.datasource.password=your_password

huggingface.api.key=hf_xxxxxxxxxxxxxxxxx
```

---

# Conclusion

This project demonstrates:

* Full-stack development
* AI integration
* LLM prompt engineering
* backend architecture
* frontend-backend communication
* real-world debugging
* production-style problem solving

The platform successfully automates the complete technical recruitment screening workflow using Generative AI.
