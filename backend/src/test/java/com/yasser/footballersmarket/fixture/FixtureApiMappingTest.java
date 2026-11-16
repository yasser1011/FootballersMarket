package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.Games;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureApiMappingTest {

    @Test
    void shouldCountAnyMinutesAsAnAppearance() {
        assertThat(FixtureApiMapping.appeared(new Games(1, "6.3"))).isTrue();
        assertThat(FixtureApiMapping.appeared(new Games(90, "7.2"))).isTrue();
    }

    @Test
    void shouldNotCountAnUnusedSub() {
        assertThat(FixtureApiMapping.appeared(new Games(0, "0"))).isFalse();
        assertThat(FixtureApiMapping.appeared(new Games(null, null))).isFalse();
        assertThat(FixtureApiMapping.appeared(null)).isFalse();
    }

    @Test
    void shouldUseTheApiRatingWhenItParses() {
        assertThat(FixtureApiMapping.matchRating(new Games(90, "7.2"))).isEqualTo(7.2);
    }

    @Test
    void shouldFallBackToTheDefaultWhenThereIsNoUsableRating() {
        double expected = FixtureResultConstants.DEFAULT_MATCH_RATING;

        assertThat(FixtureApiMapping.matchRating(new Games(20, null))).isEqualTo(expected);   // played, unrated
        assertThat(FixtureApiMapping.matchRating(new Games(20, "0"))).isEqualTo(expected);    // api's "none" value
        assertThat(FixtureApiMapping.matchRating(new Games(20, "n/a"))).isEqualTo(expected);  // garbage
        assertThat(FixtureApiMapping.matchRating(null)).isEqualTo(expected);
    }
}
