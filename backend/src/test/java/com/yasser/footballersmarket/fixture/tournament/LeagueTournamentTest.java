package com.yasser.footballersmarket.fixture.tournament;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.LeagueStandingsDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LeagueTournamentTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    @Test
    void shouldFilterFixturesOnImportantTeamsAndTopRankedTeams() throws IOException {

        FixtureResponseDto mockFixtureResponse;
        try (InputStream input = getClass()
                .getResourceAsStream("/fixture/fixturesresponse.json")) {

            assertNotNull(input, "Fixture JSON file was not found");

            mockFixtureResponse = objectMapper.readValue(
                    input,
                    FixtureResponseDto.class
            );
        }

        // 52 in top ranked teams but limit is only 1
        LeagueTeam.Team[] topRankedTeams = new LeagueTeam.Team[]{new LeagueTeam.Team(35L), new LeagueTeam.Team(52L)};
        LeagueTeam.Team[] standardLeagueTeams = new LeagueTeam.Team[]{
                new LeagueTeam.Team(42L),
                new LeagueTeam.Team(50L),
                new LeagueTeam.Team(35L),
                new LeagueTeam.Team(49L)
        };
        Tournament leagueTournamentTest = new LeagueTournament(35L, 1, standardLeagueTeams);

        // 2
        FixtureResponseDto fixtureFilterResult = leagueTournamentTest
                .filterImportantFixtures(mockFixtureResponse, topRankedTeams);

        assertThat(fixtureFilterResult.response().length).isEqualTo(2);


        // check it includes only the fit teams
        // and doesnt include unfit teams 52
        boolean errorIndicator = false;
        boolean firstFixtureIndicator = false;
        boolean thirdFixtureIndicator = false;
        for(FixtureResponseDto.Response fixture: fixtureFilterResult.response()){
            Long awayId = fixture.teams().away().id();
            Long homeId = fixture.teams().home().id();
            if(homeId == 35L && awayId == 746L){
                if(firstFixtureIndicator){ errorIndicator = true;break;}
                firstFixtureIndicator = true;
            }else if(homeId == 65L && awayId == 42L){
                if(thirdFixtureIndicator){ errorIndicator = true;break;}
                thirdFixtureIndicator = true;
            }else{
                errorIndicator = true; break;
            }
        }
        assertThat(errorIndicator).isFalse();
    }
}