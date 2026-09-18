package com.apihub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications",
       indexes = @Index(name = "idx_notif_user_read", columnList = "user_id,is_read"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String message;

    /** Nullable: a notification may be system-level rather than article-level. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private NewsArticle article;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() { }

    public Notification(User user, String title, String message, NewsArticle article) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.article = article;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NewsArticle getArticle() { return article; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}
