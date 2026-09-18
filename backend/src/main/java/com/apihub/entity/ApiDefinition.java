package com.apihub.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A catalogue entry in the API Explorer ("Weather API", "Currency API", ...).
 * Named ApiDefinition rather than Api because `Api` alone is too vague next to
 * the dozens of other "api" identifiers in this codebase.
 */
@Entity
@Table(name = "apis")
@EntityListeners(AuditingEntityListener.class)
public class ApiDefinition {

    public enum AuthType { NONE, API_KEY, BEARER, BASIC }
    public enum Status { ACTIVE, DEPRECATED, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 600)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "base_url", nullable = false, length = 400)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_type", nullable = false, length = 20)
    private AuthType authenticationType = AuthType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy = "api", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiEndpoint> endpoints = new ArrayList<>();

    @CreatedDate @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate @Column(name = "updated_at")
    private Instant updatedAt;

    protected ApiDefinition() { }

    public ApiDefinition(String name, String description, String baseUrl, AuthType authenticationType) {
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.authenticationType = authenticationType;
    }

    /** Keeps both sides of the bidirectional relationship in sync. */
    public void addEndpoint(ApiEndpoint endpoint) {
        endpoints.add(endpoint);
        endpoint.setApi(this);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public AuthType getAuthenticationType() { return authenticationType; }
    public void setAuthenticationType(AuthType t) { this.authenticationType = t; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<ApiEndpoint> getEndpoints() { return endpoints; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
