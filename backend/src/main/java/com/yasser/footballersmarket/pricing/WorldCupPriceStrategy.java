package com.yasser.footballersmarket.pricing;

import com.yasser.footballersmarket.worldcup.WorldCupPriceCalculator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// tournament pricing, active for ALL players while the world cup runs:
// - participants: frozen baseRating anchor x their live tournament rating
// - everyone else: league rating decides the category, price stays flat at base
// pure math, no db access (the world cup stats arrive on the player entity)
@Component
@Primary
public class WorldCupPriceStrategy implements PriceStrategy {

    @Override
    public Integer calculatePrice(PlayerPricingData data) {
        if (data.worldCupBaseRating() != null) {
            return WorldCupPriceCalculator.currentPrice(
                    data.worldCupBaseRating(), data.worldCupRating(), data.worldCupGames());
        }
        // non-participant: flat at base from his league rating (no tournament form or games)
        return WorldCupPriceCalculator.currentPrice(data.leagueRating(), null, 0);
    }

    @Override
    public boolean isWorldCupMode() {
        return true;
    }
}
