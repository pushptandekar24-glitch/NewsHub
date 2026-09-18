package com.apihub.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsQueryTest {

    private NewsQuery query(String keyword, CategoryQuery category, String country,
                            String language, int page, int pageSize, String sort) {
        return new NewsQuery(keyword, category, country, language, page, pageSize, sort, null, null);
    }

    @Test
    @DisplayName("'world' country is normalised to null (= global feed)")
    void worldMeansGlobal() {
        assertThat(query(null, null, "World", "en", 0, 20, null).country()).isNull();
    }

    @Test
    @DisplayName("country and language codes are lowercased")
    void codesLowercased() {
        NewsQuery q = query(null, null, "IN", "EN", 0, 20, null);
        assertThat(q.country()).isEqualTo("in");
        assertThat(q.language()).isEqualTo("en");
    }

    @Test
    @DisplayName("page size is clamped between 1 and MAX_PAGE_SIZE")
    void pageSizeIsClamped() {
        assertThat(query(null, null, null, null, 0, 5000, null).pageSize()).isEqualTo(NewsQuery.MAX_PAGE_SIZE);
        assertThat(query(null, null, null, null, 0, 0, null).pageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("negative pages floor at zero and convert to 1-based for providers")
    void pageConversion() {
        NewsQuery q = query(null, null, null, null, -3, 20, null);
        assertThat(q.page()).isZero();
        assertThat(q.providerPage()).isEqualTo(1);
    }

    @Test
    @DisplayName("a missing 'from' defaults to a recent window, never unbounded")
    void freshnessDefaultApplied() {
        NewsQuery q = query(null, null, null, null, 0, 20, null);
        assertThat(q.from()).isNotNull();
        assertThat(q.from()).isAfter(Instant.now().minus(NewsQuery.DEFAULT_FRESHNESS_DAYS + 1, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("an explicit keyword wins over the category query")
    void keywordBeatsCategoryQuery() {
        CategoryQuery category = new CategoryQuery("technology", "technology", "technology",
                List.of(), List.of(), false);
        NewsQuery q = query("tesla", category, null, "en", 0, 20, null);

        assertThat(q.isSearch()).isTrue();
        assertThat(q.effectiveQuery()).isEqualTo("tesla");
    }

    @Test
    @DisplayName("fetchSize over-fetches so filtering does not leave a half-empty page")
    void fetchSizeOverFetches() {
        assertThat(query(null, null, null, null, 0, 20, null).fetchSize()).isGreaterThan(20);
        assertThat(query(null, null, null, null, 0, 5, null).fetchSize()).isGreaterThanOrEqualTo(30);
    }
}
