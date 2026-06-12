package com.yasser.footballersmarket.worldcup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// golden values agreed in the economy design: any change to the formula or
// the constants must consciously update these numbers
class WorldCupPriceCalculatorTest {

    @Test
    void basePriceCategories() {
        assertThat(WorldCupPriceCalculator.basePriceFor(7.50)).isEqualTo(1000);
        assertThat(WorldCupPriceCalculator.basePriceFor(7.20)).isEqualTo(1000); // boundary
        assertThat(WorldCupPriceCalculator.basePriceFor(7.19)).isEqualTo(500);
        assertThat(WorldCupPriceCalculator.basePriceFor(6.80)).isEqualTo(500);  // boundary
        assertThat(WorldCupPriceCalculator.basePriceFor(6.79)).isEqualTo(250);
        assertThat(WorldCupPriceCalculator.basePriceFor(6.50)).isEqualTo(250);
        // players never seen in league stats
        assertThat(WorldCupPriceCalculator.basePriceFor(null)).isEqualTo(250);
    }

    @Test
    void priceStartsExactlyAtBase() {
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 6.5)).isEqualTo(1000);
        assertThat(WorldCupPriceCalculator.currentPrice(7.0, 6.5)).isEqualTo(500);
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 6.5)).isEqualTo(250);
        assertThat(WorldCupPriceCalculator.currentPrice(null, null)).isEqualTo(250);
    }

    @Test
    void priceMovesAggressivelyWithWorldCupRating() {
        // elite: one full rating point = +80%
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 7.5)).isEqualTo(1800);
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 7.0)).isEqualTo(1342);
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 8.0)).isEqualTo(2415);
        // medium
        assertThat(WorldCupPriceCalculator.currentPrice(6.9, 7.0)).isEqualTo(671);
        // low breakout: unknown player having a great tournament
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 8.0)).isEqualTo(604);
    }

    @Test
    void priceIsClampedAtFloorAndCap() {
        // 1.8^2.5 = 4.35 -> capped at 4x
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 9.0)).isEqualTo(4000);
        // 1.8^-1.5 = 0.41 -> floored at 0.5x
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 5.0)).isEqualTo(125);
    }
}
