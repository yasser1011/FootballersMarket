package com.yasser.footballersmarket.fixture;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// the two nullable latches mean opposite things for a row that predates the column, and getting the
// default wrong silently skips (or replays) every fixture, so both defaults are pinned here
class FixtureTest {

    private Fixture newFixture() {
        return new Fixture(1L, Instant.now(), 39L, "r", "NS", 1L, 2L, null, null, null);
    }

    @Test
    void shouldStartANewFixtureWithStatsNotYetApplied() {
        assertThat(newFixture().isStatsApplied()).isFalse();
    }

    @Test
    void shouldReadAStatsAppliedNullFromAnOldRowAsAlreadyApplied() {
        Fixture oldRow = newFixture();
        // what hibernate does to a row created before the column existed: the db null overwrites
        // the field initializer
        ReflectionTestUtils.setField(oldRow, "statsApplied", null);

        assertThat(oldRow.isStatsApplied()).isTrue();
    }

    @Test
    void shouldReadAnImportantNullFromAnOldRowAsImportant() {
        assertThat(newFixture().isImportant()).isTrue();
    }

    @Test
    void shouldHoldTheLatchOnceSet() {
        Fixture fixture = newFixture();

        fixture.setStatsApplied(true);

        assertThat(fixture.isStatsApplied()).isTrue();
    }
}
