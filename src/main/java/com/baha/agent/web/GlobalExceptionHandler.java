package com.baha.agent.web;

import com.baha.agent.agent.AgentUpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Error mapping for the API:
 * <ul>
 *   <li>{@link AgentUpstreamException} → 502 (model/tool-loop failure — not our bug)</li>
 *   <li>any other {@link RuntimeException} → 500, logged (a genuine defect, surfaced
 *       distinctly rather than masked as an upstream error)</li>
 *   <li>bean-validation failures → 400 via Spring's default handler (not a RuntimeException)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AgentUpstreamException.class)
    public ResponseEntity<Map<String, String>> handleUpstreamFailure(AgentUpstreamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "The agent could not complete your request (upstream error)."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(RuntimeException ex) {
        log.error("Unexpected internal error handling request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error."));
    }
}
