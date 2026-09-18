package com.apihub.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One row per API Explorer request. This table is the ONLY source for the
 * analytics dashboard — no numbers are invented anywhere in this project.
 */
@Entity
@Table(name = "request_history",
       indexes = {
           @Index(name = "idx_reqhist_user", columnList = "user_id"),
           @Index(name = "idx_reqhist_created", columnList = "created_at")
       })
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id")
    private ApiDefinition api;

    @Column(nullable = false, length = 400)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    /** Milliseconds, measured server-side around the outbound call. */
    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected RequestHistory() { }

    public RequestHistory(User user, ApiDefinition api, String endpoint,
                          String httpMethod, int statusCode, long responseTimeMs) {
        this.user = user;
        this.api = api;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public ApiDefinition getApi() { return api; }
    public String getEndpoint() { return endpoint; }
    public String getHttpMethod() { return httpMethod; }
    public int getStatusCode() { return statusCode; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public Instant getCreatedAt() { return createdAt; }
}
