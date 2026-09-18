package com.apihub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saved_articles",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_saved_user_article", columnNames = {"user_id", "article_id"}))
public class SavedArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private NewsArticle article;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt = Instant.now();

    protected SavedArticle() { }

    public SavedArticle(User user, NewsArticle article) {
        this.user = user;
        this.article = article;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public NewsArticle getArticle() { return article; }
    public Instant getSavedAt() { return savedAt; }
}
