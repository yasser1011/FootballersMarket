package com.yasser.footballersmarket.pricing;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

// the club market price formula, extracted verbatim from PlayerDetailsResDto.getPrice()
// (dead goals/assists components dropped - they were computed and discarded).
// the same formula also lives in postgres as fn_get_price for the ranking queries
public final class ClubPriceCalculator {
    private ClubPriceCalculator() {}

    // age used when a player has no recorded date of birth (neutral: keeps normal weights)
    private static final int DEFAULT_AGE = 28;

    public static Integer calculate(LocalDate dateOfBirth, Double avgRating) {
        final int offsetValue = 100;
        double ageWeight = 0.35;
        double ratingWeight = 0.65;

        int age = age(dateOfBirth);
        if (age < 23 || age > 33) {
            ageWeight = 0.55;
            ratingWeight = 0.45;
        }

        double rating = avgRating == null ? 0.0 : avgRating;

        double ageComponent = Math.pow(1.0 / age, 3) * ageWeight * 100_000 * offsetValue;
        double ratingComponent = Math.pow(rating, 1.1) * ratingWeight * offsetValue;

        return (int) Math.round(ageComponent + ratingComponent);
    }

    private static int age(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return DEFAULT_AGE;
        LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(dateOfBirth, currentDate).getYears();
    }
}
