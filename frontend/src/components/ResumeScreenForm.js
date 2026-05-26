import React, { useState } from 'react';
import axios from 'axios';

const ResumeScreenForm = () => {

    const [file, setFile] = useState(null);
    const [jobDescription, setJobDescription] = useState('');

    const [response, setResponse] = useState(null);

    const [loading, setLoading] = useState(false);

    const [error, setError] = useState(null);

    // Interview flow states
    const [candidateAnswers, setCandidateAnswers] = useState([]);

    const [evaluationLoading, setEvaluationLoading] = useState(false);

    const [evaluationResponse, setEvaluationResponse] = useState(null);

    const handleFileChange = (event) => {
        setFile(event.target.files[0]);
    };

    const handleJobDescriptionChange = (event) => {
        setJobDescription(event.target.value);
    };

    const handleAnswerChange = (index, value) => {

        const updatedAnswers = [...candidateAnswers];

        updatedAnswers[index] = value;

        setCandidateAnswers(updatedAnswers);
    };

    const handleSubmit = async (event) => {

        event.preventDefault();

        setLoading(true);

        setError(null);

        setResponse(null);

        setEvaluationResponse(null);

        if (!file || !jobDescription) {

            setError(
                "Please upload a resume and provide a job description."
            );

            setLoading(false);

            return;
        }

        const formData = new FormData();

        formData.append('file', file);

        formData.append('jobDescription', jobDescription);

        try {

            const result = await axios.post(
                'http://localhost:8080/api/v1/screen',
                formData,
                {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                    },
                }
            );

            setResponse(result.data);

            // Initialize answer fields
            if (
                result.data.resultType === 'INTERVIEW_QUESTIONS'
            ) {

                let questions = [];

                if (typeof result.data.resultContent === 'string') {

                    questions = result.data.resultContent
                        .split('\n')
                        .filter(q => q.trim() !== '');
                }

                setCandidateAnswers(
                    new Array(questions.length).fill('')
                );
            }

        } catch (err) {

            console.error('Error screening resume:', err);

            setError(
                'Failed to screen resume. Please try again. ' +
                (err.response?.data || err.message)
            );

        } finally {

            setLoading(false);
        }
    };

    const handleEvaluateAnswers = async () => {

        try {

            setEvaluationLoading(true);

            setError(null);

            const payload = {
                analysisId: response.analysisId,
                questions: response.resultContent,
                answers: candidateAnswers
            };

            const result = await axios.post(
                'http://localhost:8080/api/v1/screen/evaluate',
                payload
            );

            setEvaluationResponse(result.data);

        } catch (err) {

            console.error("Evaluation failed:", err);

            setError(
                'Failed to evaluate answers. ' +
                (err.response?.data || err.message)
            );

        } finally {

            setEvaluationLoading(false);
        }
    };

    const getQuestionsArray = () => {

        if (!response?.resultContent) return [];

        try {

            const parsed =
                JSON.parse(response.resultContent);

            return parsed.questions || [];

        } catch (error) {

            console.error(
                "Failed to parse questions JSON:",
                error
            );

            return [];
        }
    };

    return (

        <div className="container mt-5 mb-5">

            <h1 className="mb-4 text-center">
                AI Resume Screener & Interview Evaluator
            </h1>

            <div className="card p-4 shadow">

                <form onSubmit={handleSubmit}>

                    <div className="mb-3">

                        <label
                            htmlFor="resumeFile"
                            className="form-label"
                        >
                            Upload Resume (PDF/DOCX)
                        </label>

                        <input
                            type="file"
                            className="form-control"
                            id="resumeFile"
                            accept=".pdf,.doc,.docx"
                            onChange={handleFileChange}
                        />

                    </div>

                    <div className="mb-3">

                        <label
                            htmlFor="jobDescription"
                            className="form-label"
                        >
                            Job Description
                        </label>

                        <textarea
                            className="form-control"
                            id="jobDescription"
                            rows="8"
                            placeholder="Paste the job description here..."
                            value={jobDescription}
                            onChange={handleJobDescriptionChange}
                        ></textarea>

                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary w-100"
                        disabled={loading}
                    >
                        {
                            loading
                                ? 'Processing...'
                                : 'Screen Resume'
                        }
                    </button>

                </form>

                {
                    error && (

                        <div
                            className="alert alert-danger mt-4"
                            role="alert"
                        >
                            {error}
                        </div>
                    )
                }

                {
                    response && (

                        <div className="mt-4 p-4 border rounded bg-light">

                            <h3 className="text-success">
                                Analysis Complete
                            </h3>

                            <p>
                                <strong>Analysis ID:</strong>
                                {' '}
                                {response.analysisId}
                            </p>

                            <p>
                                <strong>Match Score:</strong>
                                {' '}
                                {response.matchScore}%
                            </p>

                            <hr />

                            <h4>
                                {
                                    response.resultType ===
                                    'INTERVIEW_QUESTIONS'
                                        ? 'Generated Interview Questions'
                                        : 'Rejection Feedback'
                                }
                            </h4>

                            {
                                response.resultType ===
                                'INTERVIEW_QUESTIONS' ? (

                                    <div>

                                        {
                                            getQuestionsArray().map(
                                                (question, index) => (

                                                    <div
                                                        key={index}
                                                        className="mb-4"
                                                    >

                                                        <label
                                                            className="form-label fw-bold"
                                                        >
                                                            Q{index + 1}.
                                                            {' '}
                                                            {question}
                                                        </label>

                                                        <textarea
                                                            className="form-control"
                                                            rows="4"
                                                            placeholder="Candidate answer..."
                                                            value={
                                                                candidateAnswers[index] || ''
                                                            }
                                                            onChange={(e) =>
                                                                handleAnswerChange(
                                                                    index,
                                                                    e.target.value
                                                                )
                                                            }
                                                        />

                                                    </div>
                                                )
                                            )
                                        }

                                        <button
                                            className="btn btn-success w-100"
                                            onClick={
                                                handleEvaluateAnswers
                                            }
                                            disabled={
                                                evaluationLoading
                                            }
                                        >
                                            {
                                                evaluationLoading
                                                    ? 'Evaluating Answers...'
                                                    : 'Evaluate Candidate Answers'
                                            }
                                        </button>

                                    </div>

                                ) : (

                                    <p>
                                        {response.resultContent}
                                    </p>
                                )
                            }

                            <hr />

                            <h4>Initial HR Summary</h4>

                            <p>{response.hrSummary}</p>

                        </div>
                    )
                }

                {
                    evaluationResponse && (

                        <div
                            className="mt-4 p-4 border rounded bg-white shadow"
                        >

                            <h3 className="text-primary">
                                Final Candidate Evaluation
                            </h3>

                            <hr />

                            <p>
                                <strong>Overall Rating:</strong>
                                {' '}
                                {evaluationResponse.overallRating}
                            </p>

                            <p>
                                <strong>Technical Score:</strong>
                                {' '}
                                {evaluationResponse.technicalScore}/10
                            </p>

                            <p>
                                <strong>Communication Score:</strong>
                                {' '}
                                {evaluationResponse.communicationScore}/10
                            </p>

                            <p>
                                <strong>Problem Solving Score:</strong>
                                {' '}
                                {evaluationResponse.problemSolvingScore}/10
                            </p>

                            <hr />

                            <h4>Evaluator Summary</h4>

                            <p>
                                {
                                    evaluationResponse.evaluatorSummary
                                }
                            </p>

                            <hr />

                            <h4>Strengths</h4>

                            <ul>
                                {
                                    evaluationResponse.strengths?.map(
                                        (item, index) => (
                                            <li key={index}>
                                                {item}
                                            </li>
                                        )
                                    )
                                }
                            </ul>

                            <h4>Weaknesses</h4>

                            <ul>
                                {
                                    evaluationResponse.weaknesses?.map(
                                        (item, index) => (
                                            <li key={index}>
                                                {item}
                                            </li>
                                        )
                                    )
                                }
                            </ul>

                            <h4>Final Recommendation</h4>

                            <div
                                className={
                                    evaluationResponse.recommendation ===
                                    'SELECT'
                                        ? 'alert alert-success'
                                        : 'alert alert-danger'
                                }
                            >
                                {
                                    evaluationResponse.recommendation
                                }
                            </div>

                        </div>
                    )
                }

            </div>

        </div>
    );
};

export default ResumeScreenForm;