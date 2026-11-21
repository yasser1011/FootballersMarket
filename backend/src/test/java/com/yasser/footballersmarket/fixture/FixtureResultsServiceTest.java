package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FixtureResultsServiceTest {

    private static final Long FIXTURE_ID = 1557427L;
    private static final Long HOME_TEAM_ID = 42L;
    private static final Long AWAY_TEAM_ID = 50L;

    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private FixtureFinishedHandler firstHandler;
    @Mock
    private FixtureFinishedHandler secondHandler;

    private FixtureResultsService resultsService;

    @BeforeEach
    void setUp() {
        resultsService = new FixtureResultsService(fixtureRepository, List.of(firstHandler, secondHandler));
    }

    @Test
    void shouldHandEveryHandlerTheFixtureAndTheSameResult() {
        Fixture fixture = fixture();
        Response result = result("FT", 2, 1, true);

        resultsService.applyResult(fixture, result);

        // the very same result object, not a copy: handlers read the per-player payload from it
        verify(firstHandler).onFixtureFinished(fixture, result);
        verify(secondHandler).onFixtureFinished(fixture, result);
    }

    @Test
    void shouldSaveTheScoreAndWinnerBeforeTellingHandlers() {
        Fixture fixture = fixture();

        resultsService.applyResult(fixture, result("FT", 2, 1, true));

        assertThat(fixture.getStatus()).isEqualTo("FT");
        assertThat(fixture.getHomeGoals()).isEqualTo(2);
        assertThat(fixture.getAwayGoals()).isEqualTo(1);
        assertThat(fixture.getWinnerTeamId()).isEqualTo(HOME_TEAM_ID);
        verify(fixtureRepository).save(fixture);
    }

    @Test
    void shouldNotNotifyHandlersWhileTheMatchIsStillInPlay() {
        Fixture fixture = fixture();

        resultsService.applyResult(fixture, result("2H", 1, 1, false));

        // the in-play state is still saved so the poll sees it, but nothing reacts to it yet
        assertThat(fixture.getStatus()).isEqualTo("2H");
        verify(fixtureRepository).save(fixture);
        verify(firstHandler, never()).onFixtureFinished(any(), any());
        verify(secondHandler, never()).onFixtureFinished(any(), any());
    }

    @Test
    void shouldStillCallTheNextHandlerWhenOneFails() {
        Fixture fixture = fixture();
        Response result = result("FT", 2, 1, true);
        doThrow(new RuntimeException("boom")).when(firstHandler).onFixtureFinished(fixture, result);

        resultsService.applyResult(fixture, result);

        // a failing reaction is logged, never allowed to starve the others or undo the saved result
        verify(secondHandler).onFixtureFinished(fixture, result);
        verify(fixtureRepository).save(fixture);
    }

    private Fixture fixture() {
        return new Fixture(FIXTURE_ID, Instant.now().minusSeconds(8000), 39L, "Regular Season - 12", "NS",
                HOME_TEAM_ID, AWAY_TEAM_ID, null, null, null);
    }

    private Response result(String status, int homeGoals, int awayGoals, boolean homeWon) {
        return new Response(
                new Response.Fixture(FIXTURE_ID, null, null, new Response.Fixture.Status(status)),
                null,
                new Response.Teams(
                        new Response.Teams.Home(HOME_TEAM_ID, "home", "logo", homeWon),
                        new Response.Teams.Away(AWAY_TEAM_ID, "away", "logo", false)),
                new Response.Goals(homeGoals, awayGoals),
                null);
    }
}
