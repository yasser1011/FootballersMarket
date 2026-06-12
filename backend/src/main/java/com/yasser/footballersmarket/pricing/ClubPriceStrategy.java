package com.yasser.footballersmarket.pricing;

import org.springframework.stereotype.Component;

// regular season pricing: age + league rating formula.
// becomes @Primary again after the world cup ends
@Component
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
