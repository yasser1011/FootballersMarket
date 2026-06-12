package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Fixture;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.FixtureResult;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Goals;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.League;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Status;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Team;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Teams;
import com.yasser.footballersmarket.worldcup.dto.WorldCupFixtureResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldCupFixtureServiceTest {

    @Mock private WorldCupFixtureRepository fixtureRepository;
    @Mock private WorldCupTeamRepository teamRepository;
    @Mock private WorldCupResultsClient resultsClient;
    @InjectMocks private WorldCupFixtureService service;

    private static final long HOME = 16L, AWAY = 1531L, FIXTURE_ID = 1489369L;

    private List<WorldCupTeam> teams() {
        return List.of(new WorldCupTeam(HOME, "Mexico", 14, "logoHome"),
                new WorldCupTeam(AWAY, "South Africa", 60, "logoAway"));
    }

    @Test
    void returnsPersistedFixturesFromDbWithoutFetching() {
        WorldCupFixture fixture = new WorldCupFixture(FIXTURE_ID, Instant.now(), "Group Stage - 1", HOME, AWAY, "FT");
        fixture.setHomeGoals(2);
        fixture.setAwayGoals(0);
        fixture.setWinnerTeamId(HOME);
        when(fixtureRepository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(any(), any()))
                .thenReturn(List.of(fixture));
        when(teamRepository.findAllById(any())).thenReturn(teams());

        List<WorldCupFixtureResponse> result = service.getTodayAndTomorrowFixtures();

        assertThat(result).hasSize(1);
        WorldCupFixtureResponse r = result.get(0);
        assertThat(r.id()).isEqualTo(FIXTURE_ID);
        assertThat(r.home().name()).isEqualTo("Mexico");
        assertThat(r.home().fifaRank()).isEqualTo(14);
        assertThat(r.away().name()).isEqualTo("South Africa");
        assertThat(r.homeGoals()).isEqualTo(2);
        assertThat(r.winnerTeamId()).isEqualTo(HOME);
        verify(resultsClient, never()).fetchFixturesByDateRange(any(), any());
    }

    @Test
    void fetchesAndPersistsWhenDbHasNoneForWindow() {
        when(fixtureRepository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(any(), any()))
                .thenReturn(List.of());
        // a fixture kicking off "now" so it falls inside today+tomorrow window
        FixtureResult fetched = new FixtureResult(
                new Fixture(FIXTURE_ID, OffsetDateTime.now().toString(), new Status("NS")),
                new League("Round of 16"),
                new Teams(new Team(HOME, null), new Team(AWAY, null)),
                new Goals(null, null),
                List.of());
        when(resultsClient.fetchFixturesByDateRange(any(), any())).thenReturn(List.of(fetched));
        when(teamRepository.findAllById(any())).thenReturn(teams());

        List<WorldCupFixtureResponse> result = service.getTodayAndTomorrowFixtures();

        verify(fixtureRepository).saveAll(any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).round()).isEqualTo("Round of 16");
        assertThat(result.get(0).home().name()).isEqualTo("Mexico");
    }
}
