package com.yasser.footballersmarket.pricing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldCupPriceStrategyTest {

    private final WorldCupPriceStrategy strategy = new WorldCupPriceStrategy();

    @Test
    void participantPricedByBasePlusFormAndGames() {
        // elite anchor (7.5 last season), 7.5 avg, 0 games -> 600 + 1.0*300 = 900
        Integer price = strategy.calculatePrice(
                new PlayerPricingData(null, 7.0, 7.5, 7.5, 0));
        assertThat(price).isEqualTo(900);
    }

    @Test
    void participantGamesAddToPrice() {
        // same player after a 7-game run -> 600 + 300 + 7*200 = 2300
        Integer price = strategy.calculatePrice(
                new PlayerPricingData(null, 7.0, 7.5, 7.5, 7));
        assertThat(price).isEqualTo(2300);
    }

    @Test
    void nonParticipantStaysFlatAtBaseFromLeagueRating() {
        // elite club player whose country didn't qualify (no wc stats): flat base 600
        Integer price = strategy.calculatePrice(
                new PlayerPricingData(null, 7.3, null, null, null));
        assertThat(price).isEqualTo(600);
    }

    @Test
    void notPersistedPlayerPricedFromLeagueRating() {
        Integer price = strategy.calculatePrice(
                new PlayerPricingData(null, 6.9, null, null, null));
        assertThat(price).isEqualTo(450);
    }
}
