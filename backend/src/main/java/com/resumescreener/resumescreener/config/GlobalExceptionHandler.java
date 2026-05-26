package com.resumescreener.resumescreener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        logger.warn("Invalid argument provided: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid input provided",
                "INVALID_INPUT",
                generateErrorId()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        logger.warn("Validation failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("errorCode", "VALIDATION_ERROR");
        response.put("errorId", generateErrorId());
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        logger.warn("Missing required parameter: {}", ex.getParameterName());

        ErrorResponse errorResponse = new ErrorResponse(
                "Missing required parameter: " + ex.getParameterName(),
                "MISSING_PARAMETER",
                generateErrorId()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleFileSizeException(
            MaxUploadSizeExceededException ex,
            WebRequest request) {

        logger.warn("File size exceeded: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "File size exceeds maximum limit (50MB)",
                "FILE_SIZE_EXCEEDED",
                generateErrorId()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {

        logger.error("Runtime exception occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "An error occurred processing your request",
                "INTERNAL_ERROR",
                generateErrorId()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(
            Exception ex,
            WebRequest request) {

        logger.error("Unexpected exception occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "An unexpected error occurred",
                "UNEXPECTED_ERROR",
                generateErrorId()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String generateErrorId() {
        return UUID.randomUUID().toString();
    }

    public static class ErrorResponse {
        private String message;
        private String errorCode;
        private String errorId;
        private LocalDateTime timestamp;

        public ErrorResponse(String message, String errorCode, String errorId) {
            this.message = message;
            this.errorCode = errorCode;
            this.errorId = errorId;
            this.timestamp = LocalDateTime.now();
        }

        public String getMessage() {
            return message;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorId() {
            return errorId;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
