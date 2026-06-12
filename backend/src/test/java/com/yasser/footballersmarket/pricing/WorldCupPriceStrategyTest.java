package com.yasser.footballersmarket.pricing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldCupPriceStrategyTest {

    private final WorldCupPriceStrategy strategy = new WorldCupPriceStrategy();

    @Test
    void participantPricedByFrozenAnchorAndTournamentRating() {
        // elite anchor (7.5 last season), great tournament so far (7.5 avg) -> 1.8x
        Integer price = strategy.calculatePrice(new PlayerPricingData(null, 7.0, 7.5, 7.5));
        assertThat(price).isEqualTo(1800);
    }

    @Test
    void nonParticipantStaysFlatAtBasePriceFromLeagueRating() {
        // elite club player whose country didn't qualify (no wc stats): flat 1000 all tournament
        Integer price = strategy.calculatePrice(new PlayerPricingData(null, 7.3, null, null));
        assertThat(price).isEqualTo(1000);
    }

    @Test
    void notPersistedPlayerPricedFromLeagueRating() {
        Integer price = strategy.calculatePrice(new PlayerPricingData(null, 6.9, null, null));
        assertThat(price).isEqualTo(500);
    }
}
