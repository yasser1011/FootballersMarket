package com.yasser.footballersmarket.worldcup;

import org.junit.jupiter.api.Test;

import static com.yasser.footballersmarket.worldcup.PredictionRewardCalculator.MatchOutcome.*;
import static com.yasser.footballersmarket.worldcup.PredictionRewardCalculator.calculatePoints;
import static org.assertj.core.api.Assertions.assertThat;

// golden values agreed in the prediction economy design (v5/B). any formula or
// constant change must consciously update these numbers.
// home is the lower (better) rank in every case below, so home win = favorite pick.
class PredictionRewardCalculatorTest {

    // England(4) vs Croatia(10), g=6
    @Test
    void closeMatchup() {
        assertThat(calculatePoints(4, 10, HOME_WIN, HOME_WIN, false)).isEqualTo(113);   // favorite win
        assertThat(calculatePoints(4, 10, HOME_WIN, DRAW, false)).isEqualTo(-194);      // favorite miss
        assertThat(calculatePoints(4, 10, DRAW, DRAW, false)).isEqualTo(90);            // draw win
        assertThat(calculatePoints(4, 10, DRAW, HOME_WIN, false)).isEqualTo(-89);       // draw miss
        assertThat(calculatePoints(4, 10, AWAY_WIN, AWAY_WIN, false)).isEqualTo(216);   // underdog win
        assertThat(calculatePoints(4, 10, AWAY_WIN, HOME_WIN, false)).isEqualTo(-113);  // underdog miss
    }

    // Mexico(14) vs South Africa(60), g=46
    @Test
    void mediumGap() {
        assertThat(calculatePoints(14, 60, HOME_WIN, HOME_WIN, false)).isEqualTo(65);
        assertThat(calculatePoints(14, 60, HOME_WIN, DRAW, false)).isEqualTo(-210);
        assertThat(calculatePoints(14, 60, DRAW, DRAW, false)).isEqualTo(210);
        assertThat(calculatePoints(14, 60, DRAW, HOME_WIN, false)).isEqualTo(-48);
        assertThat(calculatePoints(14, 60, AWAY_WIN, AWAY_WIN, false)).isEqualTo(456);
        assertThat(calculatePoints(14, 60, AWAY_WIN, HOME_WIN, false)).isEqualTo(-65);
    }

    // Germany(9) vs Curacao(85), g=76 -> use 80 for the headline numbers we quoted
    @Test
    void hugeMismatch() {
        assertThat(calculatePoints(5, 85, HOME_WIN, HOME_WIN, false)).isEqualTo(60);    // favorite floor 0.5
        assertThat(calculatePoints(5, 85, HOME_WIN, DRAW, false)).isEqualTo(-224);      // favorite miss grows
        assertThat(calculatePoints(5, 85, DRAW, DRAW, false)).isEqualTo(312);
        assertThat(calculatePoints(5, 85, DRAW, HOME_WIN, false)).isEqualTo(-48);       // draw penalty floor
        assertThat(calculatePoints(5, 85, AWAY_WIN, AWAY_WIN, false)).isEqualTo(660);
        assertThat(calculatePoints(5, 85, AWAY_WIN, HOME_WIN, false)).isEqualTo(-60);   // underdog penalty floor
    }

    @Test
    void exactScoreAddsOnlyOnCorrectOutcome() {
        // correct favorite + exact score
        assertThat(calculatePoints(14, 60, HOME_WIN, HOME_WIN, true)).isEqualTo(65 + 150);
        // exact flag ignored on a miss
        assertThat(calculatePoints(14, 60, HOME_WIN, DRAW, true)).isEqualTo(-210);
    }

    @Test
    void equalRanksTreatBothWinPicksAsFavorite() {
        // g=0, no underdog exists: a win pick is the favorite branch (120 reward, -192 miss)
        assertThat(calculatePoints(20, 20, HOME_WIN, HOME_WIN, false)).isEqualTo(120);
        assertThat(calculatePoints(20, 20, AWAY_WIN, AWAY_WIN, false)).isEqualTo(120);
        assertThat(calculatePoints(20, 20, HOME_WIN, DRAW, false)).isEqualTo(-192);
    }

    @Test
    void rewardCapsHold() {
        // underdog reward caps at 720, draw at 360 for extreme gaps
        assertThat(calculatePoints(1, 99, AWAY_WIN, AWAY_WIN, false)).isEqualTo(720);
        assertThat(calculatePoints(1, 99, DRAW, DRAW, false)).isEqualTo(360);
    }
}
