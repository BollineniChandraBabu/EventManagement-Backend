package com.familywishes.exception;

/**
 * Thrown when a session is no longer the user's active session because the account allows only one
 * concurrent sign-in and a newer sign-in replaced it. The UI turns this into a "signed in
 * elsewhere" notice with a sign-out / sign-in-again action.
 */
public class ConcurrentLoginException extends RuntimeException {
  public ConcurrentLoginException(String message) {
    super(message);
  }
}
