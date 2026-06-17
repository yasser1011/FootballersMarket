package com.yasser.footballersmarket.pricing;

import java.time.LocalDate;

// every input a price strategy may need, read straight off the loaded player entity
// (or an external dto). worldCup* are null for non-participants. each strategy uses
// only its own fields, so no strategy needs database access.
public record PlayerPricingData(LocalDate dateOfBirth, Double leagueRating,
                                Double worldCupBaseRating, Double worldCupRating,
                                Integer worldCupGames) {
}
