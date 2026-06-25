package com.baha.agent.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns unexpected agent/model failures into a clean JSON error instead of a
 * raw 500 stack trace. Bean-validation failures (400) are handled separately
 * by Spring's default handler and are not caught here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleAgentFailure(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "The agent could not complete your request (upstream error)."));
    }
}
