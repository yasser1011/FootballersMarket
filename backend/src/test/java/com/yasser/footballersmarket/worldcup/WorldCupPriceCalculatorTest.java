package com.yasser.footballersmarket.worldcup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// golden values for the additive model: price = base + clamp(wcRating-6.5,-1.5,+3)*300 + games*200,
// floored at 100. base is the OPENING price only. any formula/constant change updates these.
class WorldCupPriceCalculatorTest {

    @Test
    void basePriceCategories() {
        assertThat(WorldCupPriceCalculator.basePriceFor(7.50)).isEqualTo(600);
        assertThat(WorldCupPriceCalculator.basePriceFor(7.20)).isEqualTo(600); // boundary
        assertThat(WorldCupPriceCalculator.basePriceFor(7.19)).isEqualTo(450);
        assertThat(WorldCupPriceCalculator.basePriceFor(6.80)).isEqualTo(450); // boundary
        assertThat(WorldCupPriceCalculator.basePriceFor(6.79)).isEqualTo(300);
        assertThat(WorldCupPriceCalculator.basePriceFor(6.50)).isEqualTo(300);
        // players never seen in league stats
        assertThat(WorldCupPriceCalculator.basePriceFor(null)).isEqualTo(300);
    }

    @Test
    void priceStartsExactlyAtBase() {
        // kickoff: wcRating 6.5, 0 games -> price == base
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 6.5, 0)).isEqualTo(600);
        assertThat(WorldCupPriceCalculator.currentPrice(7.0, 6.5, 0)).isEqualTo(450);
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 6.5, 0)).isEqualTo(300);
        assertThat(WorldCupPriceCalculator.currentPrice(null, null, 0)).isEqualTo(300);
    }

    @Test
    void formBonusIsAdditiveAndEqualAcrossBases() {
        // +1.0 rating = +300, regardless of base -> equal performers only differ by the base edge
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 7.5, 0)).isEqualTo(900);  // 600+300
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 7.5, 0)).isEqualTo(600);  // 300+300
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 8.0, 0)).isEqualTo(1050); // 600+450
        assertThat(WorldCupPriceCalculator.currentPrice(6.9, 7.0, 0)).isEqualTo(600);  // 450+150
    }

    @Test
    void gamesBonusRewardsDeepRuns() {
        // +200 per game; a long run outvalues a higher-rated short run
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 7.5, 7)).isEqualTo(2000); // 300+300+1400
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 6.5, 3)).isEqualTo(900);  // 300+0+600
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 8.5, 7)).isEqualTo(2600); // 600+600+1400
        // 7-game lower rating (1910) beats 2-game higher rating (1210)
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 7.2, 7)).isEqualTo(1910); // 300+210+1400
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 8.2, 2)).isEqualTo(1210); // 300+510+400
    }

    @Test
    void ratingClampAndFloor() {
        // rating delta capped at +3.0
        assertThat(WorldCupPriceCalculator.currentPrice(7.5, 9.9, 0)).isEqualTo(1500); // 600+900
        // delta floored at -1.5 -> 300-450 = -150 -> price floored at 100
        assertThat(WorldCupPriceCalculator.currentPrice(6.5, 5.0, 0)).isEqualTo(100);
    }
}
