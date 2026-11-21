package com.yasser.footballersmarket.matchpredictions;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.yasser.footballersmarket.matchpredictions.PredictionRewardCalculator.MatchOutcome.AWAY_WIN;
import static com.yasser.footballersmarket.matchpredictions.PredictionRewardCalculator.MatchOutcome.HOME_WIN;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PredictionRewardCalculatorTest {


    @Test
    void underdogWinShouldReturnHigherPointsThanAFavouriteWin() {

        // regular vs top match and regular win
        int underdogWin = PredictionRewardCalculator.calculatePoints(2, 0.25, 1, 6, AWAY_WIN, AWAY_WIN, false, false);
        int favouriteWin = PredictionRewardCalculator.calculatePoints(2, 0.25, 1, 6, HOME_WIN, HOME_WIN, false, false);


        assertThat(underdogWin).isGreaterThan(favouriteWin);
    }
}