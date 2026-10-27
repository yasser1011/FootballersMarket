package com.yasser.footballersmarket.fixture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yasser.footballersmarket.fixture.tournament.LeagueTeam;
import com.yasser.footballersmarket.fixture.tournament.LeagueTournament;
import com.yasser.footballersmarket.fixture.tournament.Tournament;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.LeagueStandingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixtureDiscoveryServiceTest {

    @Mock
    FixtureDiscoveryClient fixtureDiscoveryClient;
    @Mock
    TournamentCatalog tournamentCatalog;

    // put it here so that it can be mocked
    // instead of providing englishLeague with new RestTemplate() parameter
    @Mock
    private LeagueStandingsClient leagueStandingsClient;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    FixtureDiscoveryService fixtureDiscoveryService;

    @BeforeEach
    void setUp(){
        this.fixtureDiscoveryService = new FixtureDiscoveryService(fixtureDiscoveryClient, tournamentCatalog);
    }

    @Test
    void shouldFetchAndFilterImportantFixtures() throws Exception {
        FixtureResponseDto mockFixtureResponse;
        LeagueStandingsDto mockEnglishLeagueStandingsResponse;

        try (InputStream input = getClass()
                .getResourceAsStream("/fixture/fixturesresponse.json")) {

            assertNotNull(input, "Fixture JSON file was not found");

            mockFixtureResponse = objectMapper.readValue(
                    input,
                    FixtureResponseDto.class
            );
        }
        try (InputStream input = getClass()
                .getResourceAsStream("/fixture/leaguestandingsresponse.json")) {

            assertNotNull(input, "Standings JSON file was not found");

            mockEnglishLeagueStandingsResponse = objectMapper.readValue(
                    input,
                    LeagueStandingsDto.class
            );
        }

        Long ENGLISH_LEAGUE_ID = 39L;
        Long ARSENAL_ID = 42L;
        Long MANCITY_ID = 50L;
        Long CHELSEA_ID = 49L;
        Long MANUTD_ID = 33L;

        LeagueTeam.Team[] englishLeagueTeams = new LeagueTeam.Team[]{new LeagueTeam.Team(ARSENAL_ID),
                new LeagueTeam.Team(MANCITY_ID), new LeagueTeam.Team(MANUTD_ID),new LeagueTeam.Team(CHELSEA_ID)};
        // mock new RestTemplate() or leagueStandingsClient fetching -> need to add a team that is not in the standard teams
        when(leagueStandingsClient.fetchLeagueStandings(ENGLISH_LEAGUE_ID)).thenReturn(mockEnglishLeagueStandingsResponse);

        Tournament englishLeague = new LeagueTournament(ENGLISH_LEAGUE_ID, 4, leagueStandingsClient, englishLeagueTeams);
        when(fixtureDiscoveryClient.fetchFixtures(anyLong(), any(), any())).thenReturn(mockFixtureResponse);
        when(tournamentCatalog.activeTournaments()).thenReturn(List.of(englishLeague));


        List<FixtureResponseDto> fixtureResponseDtoList = fixtureDiscoveryService.discoverNewFixtures();
        // make sure it includes the standard teams
        // id 42 -> arsenal
        // id 50 -> man city
        // id 33 -> man utd
        // id 49 -> Chelsea
        // make sure it includes the top ranking team
        // 1- id 50 -> man city
        // 2- id 42 -> arsenal
        // 3- id 35 -> Bournemouth
        // 4- id 49 -> Chelsea

        // fixtures
        // 35 Bournemouth vs 746 Sunderland, 63 Leeds vs 33 man utd, 65 Nottingham vs 42 arsenal
        assertThat(fixtureResponseDtoList.size()).isEqualTo(1);
        assertThat(fixtureResponseDtoList.get(0).response().length).isEqualTo(3);

        boolean errorIndicator = false;
        boolean firstFixtureIndicator = false;
        boolean secondFixtureIndicator = false;
        boolean thirdFixtureIndicator = false;
        for(FixtureResponseDto.Response fixture: fixtureResponseDtoList.get(0).response()){
            Long awayId = fixture.teams().away().id();
            Long homeId = fixture.teams().home().id();
            if(homeId == 35L && awayId == 746L){
                if(firstFixtureIndicator){ errorIndicator = true;break;}
                firstFixtureIndicator = true;
            }else if(homeId == 63L && awayId == 33L){
                if(secondFixtureIndicator){ errorIndicator = true;break;}
                secondFixtureIndicator = true;
            }
            else if(homeId == 65L && awayId == 42L){
                if(thirdFixtureIndicator){ errorIndicator = true;break;}
                thirdFixtureIndicator = true;
            }else{
                errorIndicator = true; break;
            }
        }
        assertThat(errorIndicator).isFalse();
    }
}