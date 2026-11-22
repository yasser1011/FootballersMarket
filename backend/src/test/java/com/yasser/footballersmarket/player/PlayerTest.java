package com.yasser.footballersmarket.player;

import org.junit.jupiter.api.Test;

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
}
