package com.yasser.footballersmarket.worldcup;

public final class WorldCupConstants {
    private WorldCupConstants() {}

    // --- pricing categories, derived from baseRating (last season league rating snapshot) ---
    public static final double ELITE_RATING_THRESHOLD = 7.2;
    public static final double MEDIUM_RATING_THRESHOLD = 6.8;
    public static final int ELITE_BASE_PRICE = 1000;
    public static final int MEDIUM_BASE_PRICE = 500;
    public static final int LOW_BASE_PRICE = 250;

    // --- price movement on world cup rating ---
    // rating every player starts the tournament with (multiplier 1.0 -> price = base)
    public static final double DEFAULT_RATING = 6.5;
    public static final double PRICE_MOVEMENT_BASE = 1.8;
    public static final double PRICE_FLOOR_MULTIPLIER = 0.5;
    public static final double PRICE_CAP_MULTIPLIER = 4.0;

    // --- predictions (g = fifa rank gap between the two teams; favorite = better/lower rank) ---
    public static final int PREDICTION_BASE_POINTS = 120; // W

    // rewards (correct prediction)
    // favorite win: W * max(0.5, 1 - g/100)
    public static final double FAVORITE_REWARD_MIN = 0.5;
    public static final double FAVORITE_REWARD_GAP_DIVISOR = 100.0;
    // draw: W * (0.6 + g/40), capped
    public static final double DRAW_REWARD_BASE = 0.6;
    public static final double DRAW_REWARD_GAP_DIVISOR = 40.0;
    public static final int DRAW_REWARD_MAX = 360;
    // underdog win: W * (1.5 + g/20), capped
    public static final double UNDERDOG_REWARD_BASE = 1.5;
    public static final double UNDERDOG_REWARD_GAP_DIVISOR = 20.0;
    public static final int UNDERDOG_REWARD_MAX = 720;
    // exact score adds on top of a correct outcome (never on a miss)
    public static final int EXACT_SCORE_BONUS = 150;

    // penalties (wrong prediction; magnitude grows for the bet you were more expected to land)
    // favorite miss: W * (1.6 + g/300)   -- the dominant pick failing hurts most
    public static final double FAVORITE_PENALTY_BASE = 1.6;
    public static final double FAVORITE_PENALTY_GAP_DIVISOR = 300.0;
    // draw miss: W * max(0.4, 0.8 - g/100)
    public static final double DRAW_PENALTY_BASE = 0.8;
    public static final double DRAW_PENALTY_MIN = 0.4;
    public static final double DRAW_PENALTY_GAP_DIVISOR = 100.0;
    // underdog miss: W * max(0.5, 1 - g/100)  -- longshot failing was expected, costs least at big gaps
    public static final double UNDERDOG_PENALTY_BASE = 1.0;
    public static final double UNDERDOG_PENALTY_MIN = 0.5;
    public static final double UNDERDOG_PENALTY_GAP_DIVISOR = 100.0;
}
