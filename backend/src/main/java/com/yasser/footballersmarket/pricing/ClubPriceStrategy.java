package com.yasser.footballersmarket.pricing;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// regular season pricing: age + league rating formula. the active mode again since the world cup ended
@Component
@Primary
public class ClubPriceStrategy implements PriceStrategy {

    @Override
    public Integer calculatePrice(PlayerPricingData data) {
        return ClubPriceCalculator.calculate(data.dateOfBirth(), data.leagueRating());
    }

    @Override
    public boolean isWorldCupMode() {
        return false;
    }
}
