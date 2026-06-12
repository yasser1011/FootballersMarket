package com.yasser.footballersmarket.worldcup;

import static com.yasser.footballersmarket.worldcup.WorldCupConstants.*;

public final class WorldCupPriceCalculator {
    private WorldCupPriceCalculator() {}

    // category base price from the frozen last-season rating snapshot
    public static int basePriceFor(Double baseRating) {
        if (baseRating == null) return LOW_BASE_PRICE;
        if (baseRating >= ELITE_RATING_THRESHOLD) return ELITE_BASE_PRICE;
        if (baseRating >= MEDIUM_RATING_THRESHOLD) return MEDIUM_BASE_PRICE;
        return LOW_BASE_PRICE;
    }

    // price = base * 1.8^(wcRating - 6.5), multiplier clamped to [0.5, 4.0].
    // starts exactly at base price (wcRating initialized to 6.5) and moves
    // aggressively with world cup match ratings only
    public static int currentPrice(Double baseRating, Double wcRating) {
        int basePrice = basePriceFor(baseRating);
        double rating = wcRating == null ? DEFAULT_RATING : wcRating;
        double multiplier = Math.pow(PRICE_MOVEMENT_BASE, rating - DEFAULT_RATING);
        double clamped = Math.max(PRICE_FLOOR_MULTIPLIER, Math.min(PRICE_CAP_MULTIPLIER, multiplier));
        return (int) Math.round(basePrice * clamped);
    }
}
