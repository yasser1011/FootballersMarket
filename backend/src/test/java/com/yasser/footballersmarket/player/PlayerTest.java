package com.yasser.footballersmarket.player;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// the refresh rule decides, in three places (details, rankings, portfolio), whether the external
// service may overwrite a player's stats -- so its truth table is pinned here once
class PlayerTest {

    @Test
    void shouldNotRefreshAPlayerTheFixturesMaintain() {
        assertThat(Player.refreshesExternally(null, 0)).isFalse();       // seeded/rapid player
        assertThat(Player.refreshesExternally("fixture", 0)).isFalse();
        assertThat(Player.refreshesExternally("fixture", 3)).isFalse();  // missed a few, not flagged yet
        assertThat(Player.refreshesExternally(null, null)).isFalse();    // row from before the column
    }

    @Test
    void shouldRefreshAnExternallyMaintainedPlayer() {
        assertThat(Player.refreshesExternally("sofascore", 0)).isTrue();
        assertThat(Player.refreshesExternally("sofascore_buy", null)).isTrue();
    }

    @Test
    void shouldRefreshAFlaggedPlayerEvenThoughFixturesUsedToMaintainHim() {
        // his matches are no longer in our fixtures, so our numbers would freeze
        assertThat(Player.refreshesExternally("fixture", Player.MISSED_MATCHES_TO_FLAG)).isTrue();
        assertThat(Player.refreshesExternally(null, Player.MISSED_MATCHES_TO_FLAG + 3)).isTrue();
    }

    @Test
    void shouldFlagAtTheThresholdAndNotBefore() {
        Player player = new Player();
        player.setMissedMatches(Player.MISSED_MATCHES_TO_FLAG - 1);
        assertThat(player.isFlagged()).isFalse();

        player.setMissedMatches(Player.MISSED_MATCHES_TO_FLAG);
        assertThat(player.isFlagged()).isTrue();
    }

    // ---- freshness: whether a buy/sell refreshes him before pricing ----

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    @Test
    void shouldNeverRefreshAFixturePlayerBeforeATransaction() {
        // whatever the stamp says: his data is the fixtures'
        assertThat(Player.needsExternalRefresh("fixture", 0, null, NOW)).isFalse();
        assertThat(Player.needsExternalRefresh(null, 2, NOW.minus(Duration.ofDays(60)), NOW)).isFalse();
    }

    @Test
    void shouldRefreshAnExternalPlayerNeverRefreshedBefore() {
        assertThat(Player.needsExternalRefresh("sofascore", 0, null, NOW)).isTrue();
    }

    @Test
    void shouldNotRefreshAnExternalPlayerRefreshedWithinThreeDays() {
        assertThat(Player.needsExternalRefresh("sofascore_buy", 0, NOW.minus(Duration.ofHours(1)), NOW)).isFalse();
        assertThat(Player.needsExternalRefresh("fixture", Player.MISSED_MATCHES_TO_FLAG,
                NOW.minus(Duration.ofDays(3)), NOW)).isFalse();   // exactly 3 days: still current
    }

    @Test
    void shouldRefreshAnExternalPlayerOlderThanThreeDays() {
        assertThat(Player.needsExternalRefresh("sofascore", 0,
                NOW.minus(Duration.ofDays(3)).minusSeconds(1), NOW)).isTrue();
        // the tab left open for months
        assertThat(Player.needsExternalRefresh("fixture", Player.MISSED_MATCHES_TO_FLAG,
                NOW.minus(Duration.ofDays(90)), NOW)).isTrue();
    }
}
