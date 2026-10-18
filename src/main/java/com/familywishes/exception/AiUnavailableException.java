package com.familywishes.exception;

/** Every Gemini model we were allowed to try was rate limited or unavailable. */
public class AiUnavailableException extends RuntimeException {
  public AiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
