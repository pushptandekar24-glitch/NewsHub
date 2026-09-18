package com.apihub.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the reported bug: clicking Cricket returned MLB, NFL and
 * American-football stories.
 *
 * These are the tests that must stay green. They pin down the two mechanisms
 * that fix it: a targeted query, and post-fetch relevance filtering.
 */
class CricketCategoryTest {

    private CategoryQueryRegistry registry;
    private ArticleRelevanceFilter filter;
    private CategoryQuery cricket;

    @BeforeEach
    void setUp() {
        registry = new CategoryQueryRegistry();
        filter = new ArticleRelevanceFilter();
        cricket = registry.find("cricket").orElseThrow();
    }

    private NormalizedArticle article(String title, String description) {
        return new NormalizedArticle(title, description, "Example Wire", null, null,
                "https://example.com/" + title.hashCode(), Instant.now(),
                "cricket", null, "en", "newsapi");
    }

    @Test
    @DisplayName("Cricket does NOT simply map to the sports category query")
    void cricketHasItsOwnQuery() {
        assertThat(cricket.query()).containsIgnoringCase("cricket");
        assertThat(cricket.query()).containsIgnoringCase("IPL");
        assertThat(cricket.query()).containsIgnoringCase("BCCI");
        assertThat(cricket.strict()).isTrue();
    }

    @Test
    @DisplayName("real cricket stories are kept")
    void keepsCricketStories() {
        List<NormalizedArticle> input = List.of(
                article("India beat Australia in T20 series decider",
                        "Rohit Sharma scored 78 as India chased down the target."),
                article("IPL 2026 auction: full list of retained players",
                        "BCCI confirmed the retention deadline."),
                article("Ashes preview: England name squad for the first Test",
                        "Root and Stokes return for the Test match at Lord's."));

        assertThat(filter.filter(input, cricket)).hasSize(3);
    }

    @Test
    @DisplayName("MLB, NFL and NBA stories are removed from the Cricket feed")
    void rejectsAmericanSports() {
        List<NormalizedArticle> input = List.of(
                article("Yankees clinch playoff spot with late home run",
                        "MLB roundup: the Yankees beat the Red Sox 6-4."),
                article("NFL Week 3: quarterback throws four touchdowns",
                        "American football recap from Sunday's games."),
                article("NBA season preview: basketball's biggest storylines",
                        "Every NBA team ranked ahead of tip-off."));

        assertThat(filter.filter(input, cricket)).isEmpty();
    }

    @Test
    @DisplayName("a mixed sports round-up mentioning baseball is rejected, not shown")
    void rejectsMixedRoundupContainingExcludedSport() {
        List<NormalizedArticle> input = List.of(
                article("Sports roundup: cricket, baseball and tennis results",
                        "A look at the weekend including MLB scores."));

        // excludeAny wins over a cricket keyword match — an empty Cricket page is
        // better than a wrong one, which is exactly what the empty state is for.
        assertThat(filter.filter(input, cricket)).isEmpty();
    }

    @Test
    @DisplayName("non-strict categories are not filtered at all")
    void nonStrictCategoriesPassThrough() {
        CategoryQuery world = registry.find("world").orElseThrow();
        List<NormalizedArticle> input = List.of(article("Anything at all", "Any description"));

        assertThat(filter.filter(input, world)).hasSize(1);
    }

    @Test
    @DisplayName("every one of the 25 categories has a query defined")
    void allCategoriesHaveQueries() {
        List<String> slugs = List.of("world", "india", "politics", "business", "finance",
                "technology", "ai", "cybersecurity", "science", "space", "health",
                "environment", "sports", "cricket", "gaming", "entertainment", "music",
                "automotive", "travel", "education", "startups", "social-media", "law",
                "agriculture", "lifestyle");

        assertThat(slugs).hasSize(25);
        slugs.forEach(slug -> assertThat(registry.find(slug))
                .as("category %s", slug)
                .isPresent());
    }
}
