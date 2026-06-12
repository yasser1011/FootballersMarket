package com.yasser.footballersmarket.pricing;

// strategy selection is done with @Primary on the active implementation:
// consumers inject PriceStrategy and get the current pricing mode.
// switching modes (world cup <-> club) = moving the @Primary annotation + deploy
public interface PriceStrategy {

    Integer calculatePrice(PlayerPricingData data);

    // lets consumers that can't branch on price alone (ranking native queries)
    // know which mode is active
    boolean isWorldCupMode();
}
