package com.apihub.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The reported complaint was that a day-old article outranked a story published
 * twenty minutes ago. This pins the recency curve that prevents that.
 */
class TrendingServiceTest {

    private final TrendingService service = new TrendingService();

    @Test
    @DisplayName("a 20-minute-old story scores far above a 24-hour-old story")
    void recencyDominates() {
        Instant now = Instant.now();
        double fresh = service.recencyScore(now.minus(20, ChronoUnit.MINUTES), now);
        double dayOld = service.recencyScore(now.minus(24, ChronoUnit.HOURS), now);

        assertThat(fresh).isGreaterThan(0.9);
        assertThat(dayOld).isLessThan(0.1);
        assertThat(fresh).isGreaterThan(dayOld * 5);
    }

    @Test
    @DisplayName("recency halves roughly every six hours")
    void halfLifeIsSixHours() {
        Instant now = Instant.now();
        double atZero = service.recencyScore(now, now);
        double atSix = service.recencyScore(now.minus(6, ChronoUnit.HOURS), now);

        assertThat(atZero).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
        assertThat(atSix).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.02));
    }

    @Test
    @DisplayName("an article with no timestamp scores zero rather than blowing up")
    void nullTimestampIsSafe() {
        assertThat(service.recencyScore(null, Instant.now())).isZero();
    }
}
