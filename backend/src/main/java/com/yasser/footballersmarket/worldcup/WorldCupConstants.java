package com.yasser.footballersmarket.worldcup;

import java.util.HashSet;
import java.util.Set;

public final class WorldCupConstants {
    private WorldCupConstants() {}

    // --- rapid api world cup identifiers (api-football league 1, edition 2026) ---
    public static final String WC_RAPID_LEAGUE_ID = "1";
    public static final String WC_RAPID_SEASON = "2026";

    // --- results polling (api status short codes) ---
    // statuses meaning the match is over -> settle on these
    public static final Set<String> FINISHED_STATUSES = Set.of("FT", "AET", "PEN");
    // statuses meaning the match won't produce a normal live result (postponed/abandoned/technical)
    // -> hard-stopped: excluded from polling so they aren't re-checked forever
    public static final Set<String> DEAD_STATUSES = Set.of("PST", "CANC", "ABD", "SUSP", "INT", "TBD", "AWD", "WO");
    // a fixture in any of these is finished-or-dead -> the results poll skips it. everything else
    // (incl. in-play 1H/HT/2H/ET/BT/P and not-started NS) stays in the poll until it reaches one.
    public static final Set<String> EXCLUDED_FROM_RESULTS_POLL = buildExcludedFromResultsPoll();
    // pause between consecutive rapid api calls in a polling batch, to stay under the rate limit
    public static final long RESULTS_RATE_LIMIT_MS = 10_000L;

    private static Set<String> buildExcludedFromResultsPoll() {
        HashSet<String> excluded = new HashSet<>(FINISHED_STATUSES);
        excluded.addAll(DEAD_STATUSES);
        return Set.copyOf(excluded);
    }

    // --- pricing: base = OPENING price by category, from baseRating (last-season rating snapshot) ---
    public static final double ELITE_RATING_THRESHOLD = 7.2;
    public static final double MEDIUM_RATING_THRESHOLD = 6.8;
    public static final int ELITE_BASE_PRICE = 600;
    public static final int MEDIUM_BASE_PRICE = 450;
    public static final int LOW_BASE_PRICE = 300;

    // --- price = base + form bonus + games bonus (ADDITIVE; base only sets the opening price,
    //     then world cup form and tournament longevity drive the price) ---
    // rating every player starts the tournament with (form bonus 0 -> price = base)
    public static final double DEFAULT_RATING = 6.5;
    // form bonus = clamp(wcRating - 6.5, MIN_DELTA, MAX_DELTA) * RATING_BONUS_PER_POINT
    public static final int RATING_BONUS_PER_POINT = 300;
    public static final double RATING_BONUS_MIN_DELTA = -1.5;
    public static final double RATING_BONUS_MAX_DELTA = 3.0;
    // games bonus = gamesPlayed * GAME_BONUS (rewards deep runs; more games = advanced further).
    // kept modest so price is driven more by performance (form) than by attendance.
    public static final int GAME_BONUS = 75;
    // absolute price floor
    public static final int PRICE_FLOOR = 100;

    // --- predictions (g = fifa rank gap between the two teams; favorite = better/lower rank) ---
    public static final int PREDICTION_BASE_POINTS = 90; // W

    // rewards (correct prediction)
    // favorite win: W * max(0.7, 1 - g/100)
    public static final double FAVORITE_REWARD_MIN = 0.7;
    public static final double FAVORITE_REWARD_GAP_DIVISOR = 100.0;
    // draw: W * (0.6 + g/40), capped
    public static final double DRAW_REWARD_BASE = 0.6;
    public static final double DRAW_REWARD_GAP_DIVISOR = 40.0;
    public static final int DRAW_REWARD_MAX = 220;
    // underdog win: W * (1.5 + g/30), capped
    public static final double UNDERDOG_REWARD_BASE = 1.5;
    public static final double UNDERDOG_REWARD_GAP_DIVISOR = 30.0;
    public static final int UNDERDOG_REWARD_MAX = 360;
    // exact score adds on top of a correct outcome (never on a miss)
    public static final int EXACT_SCORE_BONUS = 90;

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
