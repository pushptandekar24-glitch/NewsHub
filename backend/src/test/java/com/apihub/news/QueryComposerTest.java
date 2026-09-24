package com.apihub.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueryComposerTest {

    @Test
    @DisplayName("both present -> AND-wrapped so NewsAPI /everything narrows by both")
    void combinesBaseAndBoost() {
        String result = QueryComposer.withCountryBoost("technology OR software", "Netherlands OR Dutch");
        assertThat(result).isEqualTo("(technology OR software) AND (Netherlands OR Dutch)");
    }

    @Test
    @DisplayName("only a base query -> unchanged (no country selected)")
    void baseOnly() {
        assertThat(QueryComposer.withCountryBoost("cricket OR IPL", null)).isEqualTo("cricket OR IPL");
    }

    @Test
    @DisplayName("only a country boost -> becomes the whole query (plain country browse, no category)")
    void boostOnly() {
        assertThat(QueryComposer.withCountryBoost(null, "Sweden OR Swedish"))
                .isEqualTo("Sweden OR Swedish");
    }

    @Test
    @DisplayName("neither present -> null, so the caller treats it as nothing to search for")
    void neitherPresent() {
        assertThat(QueryComposer.withCountryBoost(null, null)).isNull();
        assertThat(QueryComposer.withCountryBoost("", "  ")).isNull();
    }

    @Test
    @DisplayName("blank strings are treated as absent, not as empty group clauses")
    void blanksAreTreatedAsAbsent() {
        assertThat(QueryComposer.withCountryBoost("  ", "Germany"))
                .isEqualTo("Germany");
    }
}
