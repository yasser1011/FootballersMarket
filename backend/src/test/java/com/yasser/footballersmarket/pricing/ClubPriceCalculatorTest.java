package com.yasser.footballersmarket.pricing;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ClubPriceCalculatorTest {

    // verbatim copy of the original PlayerDetailsResDto.getPrice() + getAge(),
    // kept as the characterization reference: the extracted calculator must
    // reproduce it exactly for every input
    private int originalFormula(LocalDate dateOfBirth, double avgRating) {
        final int offsetValue = 100;
        double ageWeight = 0.35;
        double ratingWeight = 0.65;

        LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int age = Period.between(dateOfBirth, currentDate).getYears();
        if (age < 23 || age > 33) {
            ageWeight = 0.55;
            ratingWeight = 0.45;
        }

        double ageComponent = Math.pow(1.0 / age, 3) * ageWeight * 100_000 * offsetValue;
        double ratingComponent = Math.pow(avgRating, 1.1) * ratingWeight * offsetValue;
        return (int) Math.round(ageComponent + ratingComponent);
    }

    @Test
    void matchesOriginalFormulaAcrossTheRealisticInputSpace() {
        for (int age = 17; age <= 40; age++) {
            LocalDate dob = LocalDate.now().minusYears(age).minusDays(40);
            for (double rating = 0.0; rating <= 9.0; rating += 0.25) {
                int expected = originalFormula(dob, rating);
                assertThat(ClubPriceCalculator.calculate(dob, rating))
                        .as("age %s rating %s", age, rating)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    void nullRatingBehavesLikeZero() {
        LocalDate dob = LocalDate.now().minusYears(25);
        assertThat(ClubPriceCalculator.calculate(dob, null))
                .isEqualTo(ClubPriceCalculator.calculate(dob, 0.0));
    }

    @Test
    void nullDateOfBirthFallsBackToNeutralAgeInsteadOfCrashing() {
        // original code NPE'd on null dob; the calculator treats it as a 28 year old
        LocalDate age28 = LocalDate.now().minusYears(28).minusDays(40);
        assertThat(ClubPriceCalculator.calculate(null, 7.0))
                .isEqualTo(ClubPriceCalculator.calculate(age28, 7.0));
    }
}
