package com.yasser.footballersmarket.player.dto;

import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
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

    // ---- avg rating: the java price and the ranking sql (avg_rating) must agree on it, or a player
    // is bought at one price and valued at another. these pin the same cases as the sql's case expression

    @Test
    void shouldAverageLeagueAndChampionsLeagueRatings() {
        assertThat(dto(7.0, 8.0).getAvgRating()).isEqualTo(7.5);
    }

    @Test
    void shouldUseTheLeagueRatingAloneWithoutChampionsLeagueGames() {
        assertThat(dto(7.2, null).getAvgRating()).isEqualTo(7.2);
    }

    @Test
    void shouldUseTheChampionsLeagueRatingAloneWithoutALeagueRating() {
        assertThat(dto(null, 6.9).getAvgRating()).isEqualTo(6.9);
    }

    @Test
    void shouldBeZeroWithNoRatingAtAll() {
        assertThat(new PlayerDetailsResDto().getAvgRating()).isZero();
        assertThat(dto(null, null).getAvgRating()).isZero();
    }

    @Test
    void shouldRoundTheAverageToTwoDecimals() {
        // (7.04 + 7.07) / 2 = 7.055
        assertThat(dto(7.04, 7.07).getAvgRating()).isEqualTo(7.06);
    }

    private PlayerDetailsResDto dto(Double leagueRating, Double championsLeagueRating) {
        PlayerDetailsResDto dto = new PlayerDetailsResDto();
        dto.setLeagueStats(new PlayerLeagueStats(1, 0, leagueRating));
        if (championsLeagueRating != null) {
            dto.setChampionsLeagueStats(new PlayerChampionsLeagueStats(0, 0, championsLeagueRating));
        }
        return dto;
    }
}
