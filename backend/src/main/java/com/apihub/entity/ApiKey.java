package com.apihub.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * SECURITY DESIGN — read this before changing anything here.
 *
 * We store only a SHA-256 hash of the key, plus a short non-secret prefix
 * (e.g. "sk_live_a1b2c3") used for display. The full key is returned to the
 * user exactly ONCE, at creation time, and is unrecoverable afterwards.
 *
 * This is the same model GitHub/Stripe use: a database leak then exposes no
 * usable credentials.
 */
@Entity
@Table(name = "api_keys",
       uniqueConstraints = @UniqueConstraint(name = "uk_api_key_hash", columnNames = "key_hash"))
public class ApiKey {

    public enum Status { ACTIVE, REVOKED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 80)
    private String label;

    /** Safe to show in the UI. Never enough to authenticate with. */
    @Column(name = "key_prefix", nullable = false, length = 24)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ApiKey() { }

    public ApiKey(User user, String label, String keyPrefix, String keyHash, Instant expiresAt) {
        this.user = user;
        this.label = label;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getLabel() { return label; }
    public String getKeyPrefix() { return keyPrefix; }
    public String getKeyHash() { return keyHash; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
