package com.yasser.footballersmarket.worldcup;

import static com.yasser.footballersmarket.worldcup.WorldCupConstants.*;

public final class WorldCupPriceCalculator {
    private WorldCupPriceCalculator() {}

    // category base price (the opening price) from the frozen last-season rating snapshot
    public static int basePriceFor(Double baseRating) {
        if (baseRating == null) return LOW_BASE_PRICE;
        if (baseRating >= ELITE_RATING_THRESHOLD) return ELITE_BASE_PRICE;
        if (baseRating >= MEDIUM_RATING_THRESHOLD) return MEDIUM_BASE_PRICE;
        return LOW_BASE_PRICE;
    }

    // price = base + clamp(wcRating - 6.5, -1.5, +3.0) * 300 + gamesPlayed * 200, floored at 100.
    // base only sets the OPENING price (wcRating 6.5 + 0 games -> price = base); thereafter world
    // cup form and tournament longevity (deep runs play more games) drive the price additively, so
    // an in-form / long-lasting underdog can match or overtake a higher-pedigree player.
    public static int currentPrice(Double baseRating, Double wcRating, Integer gamesPlayed) {
        int base = basePriceFor(baseRating);
        double rating = wcRating == null ? DEFAULT_RATING : wcRating;
        double delta = Math.max(RATING_BONUS_MIN_DELTA,
                Math.min(RATING_BONUS_MAX_DELTA, rating - DEFAULT_RATING));
        int games = gamesPlayed == null ? 0 : gamesPlayed;
        double raw = base + delta * RATING_BONUS_PER_POINT + (double) games * GAME_BONUS;
        return (int) Math.max(PRICE_FLOOR, Math.round(raw));
    }
}
