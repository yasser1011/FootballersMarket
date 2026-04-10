package com.yasser.footballersmarket.player.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDetailsResDtoTest {
    private final PlayerDetailsResDto playerDetailsResDtoTest = new PlayerDetailsResDto();
    @ParameterizedTest(name = "{index} => firstClub={0}, secondClub={1}, expected={2}")
    @CsvSource({
            "Manchester City, Man City, true",
            "Barcelona, FC Barcelona, true",
            "Bayern Munchen, Bayern Munich, true",
            "Bayern München, Bayern Munich, true",
            "PSG, Paris Saint Germain, true",
            "Man Utd, Manchester United, true",
            "Arsenal, Chelsea, false",
            "Real Madrid, Barcelona, false",
            "Real Madrid, Real Madrid U19, true"
    })
    void testIsClubNamesSimilar(String firstClub, String secondClub, boolean expected) {
        playerDetailsResDtoTest.setRapidApiClub(firstClub);
        playerDetailsResDtoTest.setExternalServicePlayerClub(secondClub);
        boolean result = playerDetailsResDtoTest.getAreClubStatsUpdated();

        // AssertJ assertion
        assertThat(result)
                .as("Check if '%s' and '%s' are considered similar", firstClub, secondClub)
                .isEqualTo(expected);
    }

}