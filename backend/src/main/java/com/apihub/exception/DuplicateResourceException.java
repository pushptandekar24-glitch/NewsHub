package com.apihub.exception;

/** Maps to HTTP 409 Conflict — e.g. registering an email that already exists. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}
