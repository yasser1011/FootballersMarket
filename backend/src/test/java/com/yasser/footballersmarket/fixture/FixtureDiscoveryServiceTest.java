package com.yasser.footballersmarket.fixture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yasser.footballersmarket.fixture.client.FixtureDiscoveryClient;
import com.yasser.footballersmarket.fixture.client.LeagueStandingsClient;
import com.yasser.footballersmarket.fixture.tournament.*;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    @Mock
    private FixtureSavingService fixtureSavingService;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    FixtureDiscoveryService fixtureDiscoveryService;

    @BeforeEach
    void setUp(){
        this.fixtureDiscoveryService = new FixtureDiscoveryService(fixtureDiscoveryClient, tournamentCatalog, leagueStandingsClient, fixtureSavingService);
    }

    @Test
    void shouldFetchStandingsInLeagueTournament() throws InterruptedException {
        final Long SPANISH_LEAGUE_ID = 140L;
        final Long ATLETICO_MADRID_ID = 530L;
        final Long BARCELONA_ID = 529L;
        final Long REAL_MADRID_ID = 541L;
        // spy because we need real implementation of some methods -> .getId() and .isStanding() in discoverNewFixtures method
        // and also need to mock the filter method
        LeagueTournament spanishLeague = spy(new LeagueTournament(SPANISH_LEAGUE_ID, 3, 20,
                new LeagueTeam.Team[]{
                        new LeagueTeam.Team(ATLETICO_MADRID_ID),
                        new LeagueTeam.Team(BARCELONA_ID),
                        new LeagueTeam.Team(REAL_MADRID_ID)
                }));
        List<Tournament> activeTournaments = List.of(
                spanishLeague
        );

        when(tournamentCatalog.activeTournaments()).thenReturn(activeTournaments);
        // only this works with spies
        FixtureResponseDto fromFilter = new FixtureResponseDto(new FixtureResponseDto.Response[0]);
        doReturn(fromFilter).when(spanishLeague).filterImportantFixtures(any(), any());
//        when(spanishLeague.filterImportantFixtures(any(), any())).thenReturn(new FixtureResponseDto(new FixtureResponseDto.Response[0]));

        // for argument capture assertions
        List<Long> spanishLeagueStandings = List.of(BARCELONA_ID, REAL_MADRID_ID);
        when(leagueStandingsClient.fetchOrderedTeamIds(anyLong())).thenReturn(spanishLeagueStandings);
        FixtureResponseDto fetched = new FixtureResponseDto(new FixtureResponseDto.Response[0]);
        when(fixtureDiscoveryClient.fetchFixtures(anyLong(), any(), any())).thenReturn(fetched);

        fixtureDiscoveryService.discoverNewFixtures();

        // check fetch was called once
        verify(fixtureDiscoveryClient, times(1)).fetchFixtures(anyLong(), any(), any());

        // check fetch standings was called
        verify(leagueStandingsClient, times(1)).fetchOrderedTeamIds(anyLong());

        // verify the arguments of tournament.filterImportantFixtures()
        ArgumentCaptor<LeagueTeam.Team[]> teamsCaptor = ArgumentCaptor.forClass(LeagueTeam.Team[].class);
        verify(spanishLeague).filterImportantFixtures(eq(fetched), teamsCaptor.capture());

        assertThat(teamsCaptor.getValue())
                .containsExactly(new LeagueTeam.Team(BARCELONA_ID), new LeagueTeam.Team(REAL_MADRID_ID));

        // the fetched fixtures go to the saver exactly once, for the one tournament
        verify(fixtureSavingService).saveNewFixtures(eq(List.of(fetched)), any(), eq(Set.of()));
    }
    @Test
    void shouldNotFetchStandingsInKnockoutTournament() throws InterruptedException {
        Tournament championsLeague = spy(new KnockoutTournament(2L));
        List<Tournament> activeTournaments = List.of(championsLeague);

        when(tournamentCatalog.activeTournaments()).thenReturn(activeTournaments);
        FixtureResponseDto fixtureMocked = new FixtureResponseDto(new FixtureResponseDto.Response[0]);
        when(fixtureDiscoveryClient.fetchFixtures(anyLong(), any(), any())).thenReturn(fixtureMocked);

        FixtureResponseDto fixtureFilteredMocked = new FixtureResponseDto(new FixtureResponseDto.Response[0]);
        doReturn(fixtureFilteredMocked).when(championsLeague).filterImportantFixtures(any(), any());

        fixtureDiscoveryService.discoverNewFixtures();

        // make sure fetch standings was not called
        verify(leagueStandingsClient, never()).fetchOrderedTeamIds(any());

        // verify filter method was called with right arguments (fixture mocked and empty array)
        ArgumentCaptor<LeagueTeam.Team[]> teamsCaptor = ArgumentCaptor.forClass(LeagueTeam.Team[].class);
        verify(championsLeague).filterImportantFixtures(eq(fixtureMocked), teamsCaptor.capture());

        assertThat(teamsCaptor.getValue()).isEmpty();

        // the knockout fixtures reach the saver
        verify(fixtureSavingService).saveNewFixtures(eq(List.of(fixtureMocked)), any(), eq(Set.of()));
    }

    @Test
    void shouldStoreEveryFetchedFixtureAndMarkOnlyTheFilteredOnesImportant() throws InterruptedException {
        Tournament league = spy(new LeagueTournament(39L, 4, 20, new LeagueTeam.Team[0]));
        when(tournamentCatalog.activeTournaments()).thenReturn(List.of(league));
        when(leagueStandingsClient.fetchOrderedTeamIds(anyLong())).thenReturn(List.of());

        FixtureResponseDto.Response arsenalMatch = fixtureResponse(1L);
        FixtureResponseDto.Response regularMatch = fixtureResponse(2L);
        FixtureResponseDto fetched = new FixtureResponseDto(new FixtureResponseDto.Response[]{arsenalMatch, regularMatch});
        FixtureResponseDto filtered = new FixtureResponseDto(new FixtureResponseDto.Response[]{arsenalMatch});
        when(fixtureDiscoveryClient.fetchFixtures(anyLong(), any(), any())).thenReturn(fetched);
        doReturn(filtered).when(league).filterImportantFixtures(any(), any());

        fixtureDiscoveryService.discoverNewFixtures();

        // what is stored is the UNfiltered fetch; the filter only decides the important ids
        verify(fixtureSavingService).saveNewFixtures(eq(List.of(fetched)), any(), eq(Set.of(1L)));
    }

    @Test
    void shouldNotStoreATournamentWhoseFilterFailed() throws InterruptedException {
        // were it stored anyway, its fixtures would be frozen as "not important" for good
        Tournament league = spy(new LeagueTournament(39L, 4, 20, new LeagueTeam.Team[0]));
        when(tournamentCatalog.activeTournaments()).thenReturn(List.of(league));
        when(leagueStandingsClient.fetchOrderedTeamIds(anyLong())).thenReturn(List.of());
        when(fixtureDiscoveryClient.fetchFixtures(anyLong(), any(), any()))
                .thenReturn(new FixtureResponseDto(new FixtureResponseDto.Response[]{fixtureResponse(1L)}));
        doThrow(new RuntimeException("boom")).when(league).filterImportantFixtures(any(), any());

        fixtureDiscoveryService.discoverNewFixtures();

        verify(fixtureSavingService).saveNewFixtures(eq(List.of()), any(), eq(Set.of()));
    }

    private FixtureResponseDto.Response fixtureResponse(Long id) {
        return new FixtureResponseDto.Response(
                new FixtureResponseDto.Response.Fixture(id, null, null, null), null, null, null, null);
    }
}
