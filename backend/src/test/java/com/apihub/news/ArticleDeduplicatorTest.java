package com.apihub.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleDeduplicatorTest {

    private ArticleDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        deduplicator = new ArticleDeduplicator();
    }

    private NormalizedArticle article(String title, String source, String url, String provider) {
        return new NormalizedArticle(title, null, source, null, null, url,
                Instant.now(), null, null, "en", provider);
    }

    @Test
    @DisplayName("tracking parameters do not create a second copy of a story")
    void canonicalUrlIgnoresQueryParams() {
        List<NormalizedArticle> input = List.of(
                article("Fed holds rates steady", "Reuters", "https://www.reuters.com/markets/fed-holds", "newsapi"),
                article("Fed holds rates steady", "Reuters", "https://reuters.com/markets/fed-holds?utm_source=gnews", "gnews"));

        assertThat(deduplicator.dedupe(input)).hasSize(1);
    }

    @Test
    @DisplayName("the same headline from the same source at two URLs collapses to one")
    void titleAndSourceCatchSyndicatedDuplicates() {
        List<NormalizedArticle> input = List.of(
                article("Markets rally after inflation data", "Bloomberg", "https://a.com/1", "newsapi"),
                article("Markets rally after inflation data - Bloomberg", "Bloomberg", "https://b.com/2", "gnews"));

        assertThat(deduplicator.dedupe(input)).hasSize(1);
    }

    @Test
    @DisplayName("the same headline from DIFFERENT publishers is kept — that is coverage, not duplication")
    void differentSourcesAreKept() {
        List<NormalizedArticle> input = List.of(
                article("Election results announced", "Reuters", "https://a.com/1", "newsapi"),
                article("Election results announced", "BBC", "https://b.com/2", "gnews"));

        assertThat(deduplicator.dedupe(input)).hasSize(2);
    }

    @Test
    @DisplayName("the first article in the list wins, so callers sort newest-first beforehand")
    void firstOccurrenceWins() {
        NormalizedArticle newer = article("Same story", "CNN", "https://cnn.com/x", "gnews");
        NormalizedArticle older = article("Same story", "CNN", "https://cnn.com/x?ref=old", "newsapi");

        List<NormalizedArticle> result = deduplicator.dedupe(List.of(newer, older));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).provider()).isEqualTo("gnews");
    }
}
