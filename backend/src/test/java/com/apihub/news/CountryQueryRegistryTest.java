package com.apihub.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the reported bug: selecting Netherlands (and other
 * non-India countries) returned no useful articles.
 *
 * Root cause: NewsAPI's /everything endpoint — which handles almost every
 * request, since nearly all categories carry a query — has no `country`
 * parameter at all, and the code never compensated for that. This registry is
 * the fix; these tests pin that every country the UI offers actually has a
 * usable boost term, so NewsApiOrgProvider can bias /everything toward it.
 */
class CountryQueryRegistryTest {

    private CountryQueryRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CountryQueryRegistry();
    }

    @Test
    @DisplayName("Netherlands has a real boost term, not just the country code")
    void netherlandsHasBoost() {
        assertThat(registry.queryBoost("nl")).isPresent();
        assertThat(registry.queryBoost("nl").get())
                .containsIgnoringCase("Netherlands")
                .containsIgnoringCase("Dutch");
    }

    @Test
    @DisplayName("lookup is case-insensitive, matching how NewsQuery normalises codes")
    void lookupIsCaseInsensitive() {
        assertThat(registry.queryBoost("NL")).isEqualTo(registry.queryBoost("nl"));
    }

    @Test
    @DisplayName("every country the UI offers (India, Sweden, Germany, France, Japan, Canada, Australia, UK, US) has a boost")
    void requiredCountriesAllHaveBoosts() {
        List<String> required = List.of("in", "se", "de", "fr", "jp", "ca", "au", "gb", "us", "nl");
        required.forEach(code -> assertThat(registry.hasBoost(code)).as("boost for %s", code).isTrue());
    }

    @Test
    @DisplayName("every country in the Countries registry except 'world' has a boost term")
    void allSupportedCountriesHaveBoosts() {
        Countries.ALL.stream()
                .filter(c -> !"world".equals(c.code()))
                .forEach(c -> assertThat(registry.hasBoost(c.code()))
                        .as("boost for %s (%s)", c.name(), c.code())
                        .isTrue());
    }

    @Test
    @DisplayName("an unknown or null code returns empty rather than throwing")
    void unknownCodeIsSafe() {
        assertThat(registry.queryBoost("zz")).isEmpty();
        assertThat(registry.queryBoost(null)).isEmpty();
    }
}
