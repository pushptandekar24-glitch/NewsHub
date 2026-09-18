package com.apihub.exception;

/** Thrown when an upstream provider (NewsAPI, GNews, ...) fails or rejects us. */
public class ExternalApiException extends RuntimeException {
    private final int upstreamStatus;

    public ExternalApiException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = 502;
    }

    public int getUpstreamStatus() { return upstreamStatus; }
}
