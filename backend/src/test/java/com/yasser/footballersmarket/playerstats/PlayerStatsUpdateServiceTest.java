package com.yasser.footballersmarket.playerstats;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.fixture.FixtureResultConstants;
import com.yasser.footballersmarket.fixture.Team;
import com.yasser.footballersmarket.fixture.TeamRepository;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.Games;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.PlayerEntry;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.PlayerRef;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.StatGoals;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.Statistics;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.TeamPlayers;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.TeamRef;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsUpdateServiceTest {

    private static final Long FIXTURE_ID = 1557461L;
    private static final Long PREMIER_LEAGUE_ID = 39L;
    private static final Long CHAMPIONS_LEAGUE_ID = 2L;
    private static final Long ARSENAL_ID = 42L;
    private static final Long CHELSEA_ID = 49L;
    private static final Long SAKA_ID = 1460L;
    private static final Long NEW_SIGNING_ID = 999_001L;
    private static final Long RICE_ID = 2937L;

    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerLeagueStatsRepository leagueStatsRepository;
    @Mock
    private PlayerChampionsLeagueStatsRepository championsLeagueStatsRepository;
    @Mock
    private PendingPlayerRepository pendingPlayerRepository;
    @Mock
    private TournamentCatalog tournamentCatalog;
    @Mock
    private TeamRepository teamRepository;

    private PlayerStatsUpdateService service;

    @BeforeEach
    void setUp() {
        service = new PlayerStatsUpdateService(fixtureRepository, playerRepository, leagueStatsRepository,
                championsLeagueStatsRepository, pendingPlayerRepository, tournamentCatalog, teamRepository);
    }

    // ---- the running totals ----

    @Test
    void shouldAddTheMatchToAnExistingLeagueRow() {
        Fixture fixture = givenLeagueFixture();
        Player saka = playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1);
        givenKnownPlayers(saka);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "8.1", 1, 1)));

        PlayerLeagueStats row = saka.getLeagueStats();
        assertThat(row.getTotalNumOfGames()).isEqualTo(11);
        // (7.0 * 10 + 8.1) / 11
        assertThat(row.getRating()).isCloseTo(7.1, within(1e-9));
        assertThat(row.getGoals()).isEqualTo(4);
        assertThat(row.getAssists()).isEqualTo(2);
        verify(leagueStatsRepository).saveAll(List.of(row));
    }

    @Test
    void shouldRateAPlayerWhoPlayedButHasNoApiRatingAtTheDefault() {
        Fixture fixture = givenLeagueFixture();
        Player saka = playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1);
        givenKnownPlayers(saka);

        // a short cameo the api did not rate
        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 4, null, 0, 0)));

        double expected = (7.0 * 10 + FixtureResultConstants.DEFAULT_MATCH_RATING) / 11;
        assertThat(saka.getLeagueStats().getRating()).isCloseTo(expected, within(1e-9));
        assertThat(saka.getLeagueStats().getTotalNumOfGames()).isEqualTo(11);
    }

    @Test
    void shouldStartALeagueRowFromAFirstAppearance() {
        Fixture fixture = givenLeagueFixture();
        // in the player table but never had league stats, e.g. an academy debut
        Player debutant = new Player(SAKA_ID, 123, "Debutant", "Arsenal", null);
        givenKnownPlayers(debutant);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 25, "6.9", 1, 0)));

        PlayerLeagueStats row = debutant.getLeagueStats();
        assertThat(row).isNotNull();
        assertThat(row.getTotalNumOfGames()).isEqualTo(1);
        assertThat(row.getRating()).isEqualTo(6.9);
        assertThat(row.getGoals()).isEqualTo(1);
        assertThat(row.getAssists()).isZero();
        verify(leagueStatsRepository).saveAll(List.of(row));
    }

    @Test
    void shouldWriteTheChampionsLeagueRowForAChampionsLeagueFixture() {
        Fixture fixture = givenFixture(CHAMPIONS_LEAGUE_ID, true);
        Player saka = playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1);
        saka.setChampionsLeagueStats(new PlayerChampionsLeagueStats(SAKA_ID, 2, 1, 0, 7.5, saka));
        givenKnownPlayers(saka);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "6.9", 0, 1)));

        PlayerChampionsLeagueStats clRow = saka.getChampionsLeagueStats();
        assertThat(clRow.getTotalNumOfGames()).isEqualTo(3);
        assertThat(clRow.getRating()).isCloseTo((7.5 * 2 + 6.9) / 3, within(1e-9));
        assertThat(clRow.getAssists()).isEqualTo(1);
        // the league row is untouched by a european match
        assertThat(saka.getLeagueStats().getTotalNumOfGames()).isEqualTo(10);
        verify(championsLeagueStatsRepository).saveAll(List.of(clRow));
        verify(leagueStatsRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldNotCountAnUnusedSub() {
        Fixture fixture = givenLeagueFixture();
        Player saka = playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1);
        givenKnownPlayers(saka);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 0, "0", 0, 0)));

        assertThat(saka.getLeagueStats().getTotalNumOfGames()).isEqualTo(10);
        assertThat(saka.getLeagueStats().getRating()).isEqualTo(7.0);
        verify(leagueStatsRepository, never()).saveAll(anyList());
        // the fixture is still done: nobody else is waiting on it
        assertThat(fixture.isStatsApplied()).isTrue();
    }

    // ---- the latch ----

    @Test
    void shouldLatchTheFixtureOnceApplied() {
        Fixture fixture = givenLeagueFixture();
        givenKnownPlayers(playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1));

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.0", 0, 0)));

        assertThat(fixture.isStatsApplied()).isTrue();
        verify(fixtureRepository).save(fixture);
    }

    @Test
    void shouldDoNothingForAFixtureAlreadyApplied() {
        Fixture fixture = fixture(PREMIER_LEAGUE_ID);
        fixture.setStatsApplied(true);
        when(fixtureRepository.findById(FIXTURE_ID)).thenReturn(Optional.of(fixture));

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "8.0", 2, 0)));

        // a re-run must not count the match a second time
        verify(playerRepository, never()).findAllById(any());
        verify(leagueStatsRepository, never()).saveAll(anyList());
        verify(fixtureRepository, never()).save(any());
    }

    @Test
    void shouldNotLatchWhenTheApiSentNoPlayerData() {
        Fixture fixture = fixture(PREMIER_LEAGUE_ID);
        when(fixtureRepository.findById(FIXTURE_ID)).thenReturn(Optional.of(fixture));

        // finished, but the player block is not filled in yet
        service.onFixtureFinished(fixture, new Response(null, null, null, null, null));

        // left unlatched so it is still recognisable as "finished but not applied"
        assertThat(fixture.isStatsApplied()).isFalse();
        verify(fixtureRepository, never()).save(any());
    }

    // ---- unknown players ----

    @Test
    void shouldQueueAnUnknownPlayerFromALeagueFixture() {
        Fixture fixture = givenLeagueFixture();
        givenKnownPlayers();   // nobody in the lineup is in the player table
        when(pendingPlayerRepository.findAllById(any())).thenReturn(List.of());

        service.onFixtureFinished(fixture, result(entry(NEW_SIGNING_ID, 70, "6.8", 0, 0)));

        ArgumentCaptor<List<PendingPlayer>> queued = ArgumentCaptor.forClass(List.class);
        verify(pendingPlayerRepository).saveAll(queued.capture());
        PendingPlayer pending = queued.getValue().get(0);
        assertThat(pending.getId()).isEqualTo(NEW_SIGNING_ID);
        assertThat(pending.getTeamId()).isEqualTo(ARSENAL_ID);
        assertThat(pending.getFirstSeenFixtureId()).isEqualTo(FIXTURE_ID);
        // never created from a lineup: there is not enough data to build a player
        verify(leagueStatsRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldNotQueueAPlayerWhoIsAlreadyPending() {
        Fixture fixture = givenLeagueFixture();
        givenKnownPlayers();
        when(pendingPlayerRepository.findAllById(any())).thenReturn(List.of(
                new PendingPlayer(NEW_SIGNING_ID, "New Signing", ARSENAL_ID, 1L, Instant.now())));

        service.onFixtureFinished(fixture, result(entry(NEW_SIGNING_ID, 70, "6.8", 0, 0)));

        verify(pendingPlayerRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldNotQueueUnknownPlayersFromAChampionsLeagueFixture() {
        Fixture fixture = givenFixture(CHAMPIONS_LEAGUE_ID, true);
        givenKnownPlayers();

        // e.g. a benfica player: his domestic league is not tracked, so he would never get league stats
        service.onFixtureFinished(fixture, result(entry(NEW_SIGNING_ID, 90, "7.0", 0, 0)));

        verify(pendingPlayerRepository, never()).findAllById(any());
        verify(pendingPlayerRepository, never()).saveAll(anyList());
    }

    // ---- who fixtures maintain ----

    @Test
    void shouldBringASofascorePlayerBackToFixtureMaintenance() {
        Fixture fixture = givenLeagueFixture();
        Player returning = playerWithLeagueRow(SAKA_ID, "sofascore", 10, 7.0, 3, 1);
        givenKnownPlayers(returning);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.5", 0, 0)));

        assertThat(returning.getUpdatedBy()).isEqualTo(PlayerStatsUpdateService.FIXTURE_MAINTAINED);
        assertThat(returning.getLeagueStats().getTotalNumOfGames()).isEqualTo(11);
        verify(playerRepository).saveAll(List.of(returning));
    }

    @Test
    void shouldNeverTouchASofascoreBuyPlaceholder() {
        Fixture fixture = givenLeagueFixture();
        Player placeholder = playerWithLeagueRow(SAKA_ID, "sofascore_buy", 10, 7.0, 3, 1);
        givenKnownPlayers(placeholder);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "9.0", 3, 0)));

        assertThat(placeholder.getLeagueStats().getTotalNumOfGames()).isEqualTo(10);
        assertThat(placeholder.getUpdatedBy()).isEqualTo("sofascore_buy");
        verify(leagueStatsRepository, never()).saveAll(anyList());
    }

    // ---- squad state: misses, flag, club moves ----

    @Test
    void shouldCountAMissForAPlayerOfTheTeamWhoWasNotInTheSquad() {
        Fixture fixture = givenLeagueFixture();
        Player saka = atArsenal(playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1), 0);
        Player rice = atArsenal(playerWithLeagueRow(RICE_ID, null, 10, 7.0, 0, 0), 1);
        givenKnownPlayers(saka);
        when(playerRepository.findByTeamIdIn(any())).thenReturn(List.of(saka, rice));

        // only saka is in arsenal's squad
        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.0", 0, 0)));

        assertThat(rice.getMissedMatches()).isEqualTo(2);
        assertThat(rice.isFlagged()).isFalse();
        assertThat(saka.getMissedMatches()).isZero();
    }

    @Test
    void shouldFlagAPlayerOnHisFourthMissInARow() {
        Fixture fixture = givenLeagueFixture();
        Player saka = atArsenal(playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1), 0);
        Player rice = atArsenal(playerWithLeagueRow(RICE_ID, null, 10, 7.0, 0, 0), 3);
        givenKnownPlayers(saka);
        when(playerRepository.findByTeamIdIn(any())).thenReturn(List.of(saka, rice));

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.0", 0, 0)));

        assertThat(rice.getMissedMatches()).isEqualTo(Player.MISSED_MATCHES_TO_FLAG);
        assertThat(rice.isFlagged()).isTrue();
        verify(playerRepository).saveAll(List.of(rice));
    }

    @Test
    void shouldClearTheFlagWhenHeIsBackInTheSquad() {
        Fixture fixture = givenLeagueFixture();
        // flagged, and refreshed externally meanwhile (which marked him sofascore)
        Player rice = atArsenal(playerWithLeagueRow(RICE_ID, "sofascore", 10, 7.0, 0, 0), 6);
        givenKnownPlayers(rice);

        // an unused sub is enough: he is back in his club's squad
        service.onFixtureFinished(fixture, result(entry(RICE_ID, 0, "0", 0, 0)));

        assertThat(rice.getMissedMatches()).isZero();
        assertThat(rice.isFlagged()).isFalse();
        assertThat(rice.getUpdatedBy()).isEqualTo(PlayerStatsUpdateService.FIXTURE_MAINTAINED);
    }

    @Test
    void shouldNotCountMissesFromAChampionsLeagueFixture() {
        Fixture fixture = givenFixture(CHAMPIONS_LEAGUE_ID, true);
        Player saka = atArsenal(playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1), 0);
        givenKnownPlayers(saka);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.0", 0, 0)));

        // the roster is never even looked up for a european match
        verify(playerRepository, never()).findByTeamIdIn(any());
    }

    @Test
    void shouldLeaveSquadStateAloneWhenALaterFixtureOfTheTeamIsAlreadyApplied() {
        Fixture fixture = givenLeagueFixture();
        Player saka = atArsenal(playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1), 2);
        Player rice = atArsenal(playerWithLeagueRow(RICE_ID, null, 10, 7.0, 0, 0), 1);
        givenKnownPlayers(saka);
        when(playerRepository.findByTeamIdIn(any())).thenReturn(List.of(saka, rice));
        when(fixtureRepository.existsAppliedAfter(ARSENAL_ID, fixture.getDate())).thenReturn(true);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "8.0", 0, 0)));

        // the late result still counts as a game...
        assertThat(saka.getLeagueStats().getTotalNumOfGames()).isEqualTo(11);
        // ...but its squad is older news: nobody's missed count moves
        assertThat(saka.getMissedMatches()).isEqualTo(2);
        assertThat(rice.getMissedMatches()).isEqualTo(1);
    }

    @Test
    void shouldOnlyRecordTheClubOnAFirstSighting() {
        Fixture fixture = givenLeagueFixture();
        Player saka = playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1);   // no teamId yet
        saka.setSofascoreClub("Arsenal FC");
        givenKnownPlayers(saka);

        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.0", 0, 0)));

        assertThat(saka.getTeamId()).isEqualTo(ARSENAL_ID);
        // his stored club came from the seed/external data and is trusted
        assertThat(saka.getSofascoreClub()).isEqualTo("Arsenal FC");
        verify(playerRepository, never()).findMostCommonClubPhotoUrl(any(), any());
    }

    @Test
    void shouldFollowAMoveBetweenCoveredClubs() {
        Fixture fixture = givenLeagueFixture();
        Player mover = playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1);
        mover.setTeamId(CHELSEA_ID);
        mover.setSofascoreClub("Chelsea");
        mover.setClubPhotoUrl("https://365/chelsea.png");
        givenKnownPlayers(mover);
        when(teamRepository.findAllById(any())).thenReturn(List.of(new Team(ARSENAL_ID, "Arsenal", "api-football-logo")));
        when(playerRepository.findMostCommonClubPhotoUrl(ARSENAL_ID, SAKA_ID)).thenReturn("https://365/arsenal.png");

        // he turns up in arsenal's squad
        service.onFixtureFinished(fixture, result(entry(SAKA_ID, 90, "7.0", 0, 0)));

        assertThat(mover.getTeamId()).isEqualTo(ARSENAL_ID);
        assertThat(mover.getSofascoreClub()).isEqualTo("Arsenal");
        // the 365scores badge his new teammates carry, never the api-football logo
        assertThat(mover.getClubPhotoUrl()).isEqualTo("https://365/arsenal.png");
    }

    @Test
    void shouldNotCountAMissAtTheOldClubForAPlayerWhoMovedToTheOpponent() {
        Fixture fixture = givenLeagueFixture();
        Player saka = atArsenal(playerWithLeagueRow(SAKA_ID, null, 10, 7.0, 3, 1), 0);
        // left arsenal for chelsea and faces his old club straight away
        Player mover = atArsenal(playerWithLeagueRow(RICE_ID, null, 10, 7.0, 0, 0), 0);
        givenKnownPlayers(saka, mover);
        // the rosters are loaded before any move, so he is still on arsenal's
        when(playerRepository.findByTeamIdIn(any())).thenReturn(List.of(saka, mover));
        when(teamRepository.findAllById(any())).thenReturn(List.of(new Team(CHELSEA_ID, "Chelsea", "api-football-logo")));

        service.onFixtureFinished(fixture, new Response(null, null, null, null, List.of(
                new TeamPlayers(new TeamRef(ARSENAL_ID, "Arsenal"), List.of(entry(SAKA_ID, 90, "7.0", 0, 0))),
                new TeamPlayers(new TeamRef(CHELSEA_ID, "Chelsea"), List.of(entry(RICE_ID, 90, "7.0", 0, 0))))));

        assertThat(mover.getTeamId()).isEqualTo(CHELSEA_ID);
        // he left arsenal, he did not miss arsenal's match
        assertThat(mover.getMissedMatches()).isZero();
    }

    // ---- helpers ----

    private Player atArsenal(Player player, int missedMatches) {
        player.setTeamId(ARSENAL_ID);
        player.setMissedMatches(missedMatches);
        return player;
    }

    private Fixture givenLeagueFixture() {
        return givenFixture(PREMIER_LEAGUE_ID, false);
    }

    private Fixture givenFixture(Long leagueId, boolean championsLeague) {
        Fixture fixture = fixture(leagueId);
        when(fixtureRepository.findById(FIXTURE_ID)).thenReturn(Optional.of(fixture));
        when(tournamentCatalog.isChampionsLeague(leagueId)).thenReturn(championsLeague);
        return fixture;
    }

    private Fixture fixture(Long leagueId) {
        return new Fixture(FIXTURE_ID, Instant.now().minusSeconds(9000), leagueId, "Regular Season - 12", "FT",
                ARSENAL_ID, CHELSEA_ID, 2, 1, ARSENAL_ID);
    }

    private void givenKnownPlayers(Player... players) {
        when(playerRepository.findAllById(any())).thenReturn(List.of(players));
    }

    private Player playerWithLeagueRow(Long id, String updatedBy, int games, double rating, int goals, int assists) {
        Player player = new Player(id, 123, "Player " + id, "Arsenal", updatedBy);
        player.setLeagueStats(new PlayerLeagueStats(id, goals, assists, games, rating, player));
        return player;
    }

    private PlayerEntry entry(Long playerId, int minutes, String rating, int goals, int assists) {
        return new PlayerEntry(new PlayerRef(playerId, "Player " + playerId),
                List.of(new Statistics(new Games(minutes, rating), new StatGoals(goals, assists))));
    }

    private Response result(PlayerEntry... entries) {
        return new Response(null, null, null, null,
                List.of(new TeamPlayers(new TeamRef(ARSENAL_ID, "Arsenal"), List.of(entries))));
    }
}
