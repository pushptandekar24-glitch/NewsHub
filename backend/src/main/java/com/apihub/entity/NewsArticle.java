package com.apihub.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A cached copy of article METADATA returned by the external provider.
 *
 * WHY persist at all? Two reasons:
 *   1. External articles have no stable id, so /article/:id could not exist.
 *      We derive a deterministic externalId (SHA-256 of the URL) and store it.
 *   2. Saved articles, notifications and click analytics all need a local FK.
 *
 * WHAT WE DO NOT STORE: the full article body. That is the publisher's
 * copyrighted work. We keep title/description/url only, exactly what the
 * provider's terms allow, and always link back to the source.
 */
@Entity
@Table(name = "news_articles",
       uniqueConstraints = @UniqueConstraint(name = "uk_news_external_id", columnNames = "external_id"),
       indexes = {
           @Index(name = "idx_news_published", columnList = "published_at"),
           @Index(name = "idx_news_category", columnList = "category_id"),
           @Index(name = "idx_news_country", columnList = "country")
       })
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 of the canonical article URL. Makes upserts idempotent. */
    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 120)
    private String source;

    @Column(length = 200)
    private String author;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "article_url", nullable = false, length = 1000)
    private String articleUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 8)
    private String country;

    @Column(length = 8)
    private String language;

    /** Which upstream provider produced this row ("newsapi" / "gnews"). */
    @Column(length = 20)
    private String provider;

    @Column(name = "published_at")
    private Instant publishedAt;

    /** Incremented on each article-detail view — powers real (not fabricated) trending. */
    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public NewsArticle() { }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getArticleUrl() { return articleUrl; }
    public void setArticleUrl(String articleUrl) { this.articleUrl = articleUrl; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }
    public Instant getCreatedAt() { return createdAt; }
}
