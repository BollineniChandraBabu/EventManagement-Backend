package com.familywishes.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<?> handleNotFound(NotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<?> handleBad(BadRequestException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<?> handleRateLimit(RateLimitExceededException ex) {
    return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
  }

  /**
   * A superseded single-session login. 401 so the client treats it as an authentication failure,
   * but the message starts with {@code CONCURRENT_LOGIN} so the UI can show a dedicated notice and
   * let the user sign in again.
   */
  @ExceptionHandler(ConcurrentLoginException.class)
  public ResponseEntity<?> handleConcurrentLogin(ConcurrentLoginException ex) {
    return build(HttpStatus.UNAUTHORIZED, "CONCURRENT_LOGIN: " + ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
    return build(
        HttpStatus.BAD_REQUEST, ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
  }

  @ExceptionHandler(AiUnavailableException.class)
  public ResponseEntity<?> handleAiUnavailable(AiUnavailableException ex) {
    return build(
        HttpStatus.SERVICE_UNAVAILABLE,
        "The AI service is busy right now. Please try again in a minute.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> fallback(Exception ex) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  private ResponseEntity<?> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "timestamp",
                LocalDateTime.now(ZoneId.of(schedulerTimeZone)),
                "status",
                status.value(),
                "message",
                message));
  }
}
