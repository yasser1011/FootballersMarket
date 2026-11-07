package com.yasser.footballersmarket.matchpredictions;

// pure points math for a settled prediction. two independent inputs:
//
//   tier    - what the fixture is worth at all, from how important the two sides are (profile)
//   gapNorm - how lopsided it was, 0..1, used only to judge whether THIS pick was the brave one
//
// keeping them separate is deliberate: the tier already rates a mismatch low, so letting gapNorm
// damp the base as well would penalise a lopsided fixture twice. instead gapNorm only asks which
// side you backed, so backing the underdog across a big gap pays more than backing the favourite.
//
// the fixture bonus column and the zero-floor on the user balance are applied by the settlement
// service, not here.
public final class PredictionRewardCalculator {
    private PredictionRewardCalculator() {}

    public enum MatchOutcome { HOME_WIN, AWAY_WIN, DRAW }

    // indexed by tier 0..5; see FixtureTierResolver for how a fixture maps onto one
    private static final int[] CORRECT_BASE = {60, 70, 80, 110, 130, 160};
    private static final int[] WRONG_BASE   = {45, 55, 65, 85, 105, 130};

    static final double CHAMPIONS_LEAGUE_MULTIPLIER = 1.3;

    private static final double UNDERDOG_REWARD_UPLIFT = 1.2;
    private static final double FAVOURITE_REWARD_DAMP = 0.3;
    private static final double DRAW_REWARD_UPLIFT = 0.5;
    private static final double FAVOURITE_PENALTY_UPLIFT = 0.5;
    private static final double UNDERDOG_PENALTY_RELIEF = 0.4;
    private static final double DRAW_PENALTY_RELIEF = 0.3;

    public static final int EXACT_SCORE_BONUS = 90;

    // gapNorm and the positions are null when we hold no league table for the two sides (champions
    // league clubs from leagues we do not track) -> tier base only, no pick adjustment
    public static int calculatePoints(int tier, Double gapNorm,
                                      Integer homePosition, Integer awayPosition,
                                      MatchOutcome predicted, MatchOutcome actual,
                                      boolean exactScoreMatched, boolean championsLeague) {
        double gap = gapNorm == null ? 0.0 : Math.max(0.0, Math.min(1.0, gapNorm));
        boolean correct = predicted == actual;

        double points = correct
                ? CORRECT_BASE[tier] * correctMultiplier(predicted, gap, homePosition, awayPosition)
                : -WRONG_BASE[tier] * wrongMultiplier(predicted, gap, homePosition, awayPosition);

        if (championsLeague) points *= CHAMPIONS_LEAGUE_MULTIPLIER;

        int result = (int) Math.round(points);
        // the exact-score bonus only ever adds, and only on a correct outcome
        if (correct && exactScoreMatched) result += EXACT_SCORE_BONUS;
        return result;
    }

    private static double correctMultiplier(MatchOutcome predicted, double gap,
                                            Integer homePosition, Integer awayPosition) {
        if (predicted == MatchOutcome.DRAW) return 1 + DRAW_REWARD_UPLIFT * gap;
        return backedUnderdog(predicted, homePosition, awayPosition)
                ? 1 + UNDERDOG_REWARD_UPLIFT * gap
                : 1 - FAVOURITE_REWARD_DAMP * gap;
    }

    private static double wrongMultiplier(MatchOutcome predicted, double gap,
                                          Integer homePosition, Integer awayPosition) {
        if (predicted == MatchOutcome.DRAW) return 1 - DRAW_PENALTY_RELIEF * gap;
        return backedUnderdog(predicted, homePosition, awayPosition)
                ? 1 - UNDERDOG_PENALTY_RELIEF * gap
                : 1 + FAVOURITE_PENALTY_UPLIFT * gap;
    }

    // lower position number = better placed = favourite. with no table to compare, a pick counts as
    // backing the favourite, which is the conservative reading (no upset uplift)
    private static boolean backedUnderdog(MatchOutcome predicted, Integer homePosition, Integer awayPosition) {
        if (homePosition == null || awayPosition == null) return false;
        return predicted == MatchOutcome.HOME_WIN
                ? homePosition > awayPosition
                : awayPosition > homePosition;
    }
}
