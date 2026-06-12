package com.yasser.footballersmarket.worldcup;

import static com.yasser.footballersmarket.worldcup.WorldCupConstants.*;

// pure points math for a settled prediction (frozen v5/B spec).
// g = fifa rank gap; favorite = the better (lower) ranked team, ties count as favorite.
// reward scales with how improbable the pick was; penalty with how probable it was.
// the manual bonusPoints column and the zero-floor on the user balance are applied
// by the settlement service, not here.
public final class PredictionRewardCalculator {
    private PredictionRewardCalculator() {}

    public enum MatchOutcome { HOME_WIN, AWAY_WIN, DRAW }

    public static int calculatePoints(int homeFifaRank, int awayFifaRank,
                                      MatchOutcome predicted, MatchOutcome actual,
                                      boolean exactScoreMatched) {
        double g = Math.abs(homeFifaRank - awayFifaRank);
        boolean correct = predicted == actual;

        boolean predictedDraw = predicted == MatchOutcome.DRAW;
        boolean predictedFavorite = false;
        if (!predictedDraw) {
            int winnerRank = predicted == MatchOutcome.HOME_WIN ? homeFifaRank : awayFifaRank;
            int otherRank = predicted == MatchOutcome.HOME_WIN ? awayFifaRank : homeFifaRank;
            predictedFavorite = winnerRank <= otherRank; // lower rank = favorite; equal -> favorite
        }

        if (correct) {
            int reward;
            if (predictedDraw) {
                reward = (int) Math.round(Math.min(DRAW_REWARD_MAX,
                        PREDICTION_BASE_POINTS * (DRAW_REWARD_BASE + g / DRAW_REWARD_GAP_DIVISOR)));
            } else if (predictedFavorite) {
                reward = (int) Math.round(PREDICTION_BASE_POINTS
                        * Math.max(FAVORITE_REWARD_MIN, 1 - g / FAVORITE_REWARD_GAP_DIVISOR));
            } else {
                reward = (int) Math.round(Math.min(UNDERDOG_REWARD_MAX,
                        PREDICTION_BASE_POINTS * (UNDERDOG_REWARD_BASE + g / UNDERDOG_REWARD_GAP_DIVISOR)));
            }
            // exact score only ever adds, and only on a correct outcome
            if (exactScoreMatched) reward += EXACT_SCORE_BONUS;
            return reward;
        }

        double magnitude;
        if (predictedDraw) {
            magnitude = PREDICTION_BASE_POINTS
                    * Math.max(DRAW_PENALTY_MIN, DRAW_PENALTY_BASE - g / DRAW_PENALTY_GAP_DIVISOR);
        } else if (predictedFavorite) {
            magnitude = PREDICTION_BASE_POINTS * (FAVORITE_PENALTY_BASE + g / FAVORITE_PENALTY_GAP_DIVISOR);
        } else {
            magnitude = PREDICTION_BASE_POINTS
                    * Math.max(UNDERDOG_PENALTY_MIN, UNDERDOG_PENALTY_BASE - g / UNDERDOG_PENALTY_GAP_DIVISOR);
        }
        return -(int) Math.round(magnitude);
    }
}
