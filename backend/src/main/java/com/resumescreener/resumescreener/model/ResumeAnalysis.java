package com.resumescreener.resumescreener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "job_description", nullable = false, columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @JdbcTypeCode(SqlTypes.JSON) // Use JSON type for structured_analysis
    @Column(name = "structured_analysis", nullable = false, columnDefinition = "JSONB")
    private AIAnalysisResponse structuredAnalysis;

    @Column(name = "final_result", nullable = false, columnDefinition = "TEXT")
    private String finalResult; // Interview questions or rejection feedback

    @Column(name = "hr_summary", nullable = false, columnDefinition = "TEXT")
    private String hrSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
