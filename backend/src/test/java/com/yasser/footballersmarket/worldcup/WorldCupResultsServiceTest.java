package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStatsRepository;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Fixture;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.FixtureResult;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Games;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Goals;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.PlayerEntry;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.PlayerRef;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.StatGoals;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Statistics;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Status;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Team;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.TeamPlayers;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Teams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldCupResultsServiceTest {

    @Mock private WorldCupFixtureRepository fixtureRepository;
    @Mock private PlayerWorldCupStatsRepository playerWorldCupStatsRepository;
    @Mock private WorldCupSettlementService settlementService;
    @Mock private WorldCupFeaturedPlayerService featuredPlayerService;
    @InjectMocks private WorldCupResultsService service;

    private static final long HOME = 16L, AWAY = 1531L, FIXTURE_ID = 1489369L, P1 = 270774L;

    private WorldCupFixture fixture() {
        return new WorldCupFixture(FIXTURE_ID, Instant.now(), "Group Stage - 1", HOME, AWAY, "NS");
    }

    private PlayerWorldCupStats stats(double rating, int games, int goals, int assists) {
        return new PlayerWorldCupStats(HOME, 7.0, goals, assists, games, rating, null);
    }

    private FixtureResult result(String status, Boolean homeWinner, Boolean awayWinner,
                                 Integer homeGoals, Integer awayGoals, TeamPlayers... players) {
        return new FixtureResult(
                new Fixture(FIXTURE_ID, null, new Status(status, null)),
                null,
                new Teams(new Team(HOME, homeWinner), new Team(AWAY, awayWinner)),
                new Goals(homeGoals, awayGoals),
                List.of(players));
    }

    private TeamPlayers team(PlayerEntry... entries) {
        return new TeamPlayers(List.of(entries));
    }

    private PlayerEntry player(long id, String rating, Integer goals, Integer assists) {
        return new PlayerEntry(new PlayerRef(id, "player " + id),
                List.of(new Statistics(new Games(rating), new StatGoals(goals, assists))));
    }

    @Test
    void finishedMatchRecordsResultWinnerAndSettles() {
        WorldCupFixture fixture = fixture();

        service.processFixtureResult(fixture, result("FT", true, false, 2, 0));

        assertThat(fixture.getHomeGoals()).isEqualTo(2);
        assertThat(fixture.getAwayGoals()).isZero();
        assertThat(fixture.getWinnerTeamId()).isEqualTo(HOME);
        assertThat(fixture.getStatus()).isEqualTo("FT");
        verify(fixtureRepository).save(fixture);
        verify(settlementService).settleFixture(fixture);
    }

    @Test
    void drawWhenNeitherTeamWon() {
        WorldCupFixture fixture = fixture();

        service.processFixtureResult(fixture, result("FT", false, false, 1, 1));

        assertThat(fixture.getWinnerTeamId()).isNull();
        verify(settlementService).settleFixture(fixture);
    }

    @Test
    void firstGameUsesMatchRatingDirectly() {
        PlayerWorldCupStats s = stats(6.5, 0, 0, 0); // seeded anchor, no games yet
        when(playerWorldCupStatsRepository.findById(P1)).thenReturn(Optional.of(s));

        service.processFixtureResult(fixture(), result("FT", true, false, 1, 0,
                team(player(P1, "7.0", 1, 0))));

        assertThat(s.getRating()).isEqualTo(7.0);
        assertThat(s.getTotalNumOfGames()).isEqualTo(1);
        assertThat(s.getGoals()).isEqualTo(1);
        verify(playerWorldCupStatsRepository).save(s);
    }

    @Test
    void laterGameUpdatesRunningAverage() {
        PlayerWorldCupStats s = stats(7.0, 1, 0, 0); // avg 7.0 over 1 game
        when(playerWorldCupStatsRepository.findById(P1)).thenReturn(Optional.of(s));

        service.processFixtureResult(fixture(), result("FT", true, false, 3, 1,
                team(player(P1, "8.5", 0, 0))));

        // (7.0*1 + 8.5) / 2 = 7.75
        assertThat(s.getRating()).isEqualTo(7.75);
        assertThat(s.getTotalNumOfGames()).isEqualTo(2);
    }

    @Test
    void goalsAndAssistsAccumulateAcrossGames() {
        PlayerWorldCupStats s = stats(7.0, 1, 1, 2);
        when(playerWorldCupStatsRepository.findById(P1)).thenReturn(Optional.of(s));

        service.processFixtureResult(fixture(), result("FT", true, false, 2, 0,
                team(player(P1, "7.5", 2, 1))));

        assertThat(s.getGoals()).isEqualTo(3);
        assertThat(s.getAssists()).isEqualTo(3);
    }

    @Test
    void unusedSubWithNullRatingIsNotCounted() {
        service.processFixtureResult(fixture(), result("FT", true, false, 1, 0,
                team(player(P1, null, null, null))));

        verify(playerWorldCupStatsRepository, never()).save(any());
    }

    @Test
    void untrackedPlayerIsSkipped() {
        when(playerWorldCupStatsRepository.findById(P1)).thenReturn(Optional.empty());

        service.processFixtureResult(fixture(), result("FT", true, false, 1, 0,
                team(player(P1, "7.0", 1, 0))));

        verify(playerWorldCupStatsRepository, never()).save(any());
    }

    @Test
    void inPlayRecordsLiveScoreAndDoesNotSettle() {
        WorldCupFixture fixture = fixture();

        service.processFixtureResult(fixture, result("1H", null, null, 0, 1));

        // live score + status persisted mid-match, but no winner/settlement yet
        assertThat(fixture.getStatus()).isEqualTo("1H");
        assertThat(fixture.getHomeGoals()).isZero();
        assertThat(fixture.getAwayGoals()).isEqualTo(1);
        assertThat(fixture.getWinnerTeamId()).isNull();
        verify(fixtureRepository).save(fixture);
        verify(settlementService, never()).settleFixture(any());
    }
}
