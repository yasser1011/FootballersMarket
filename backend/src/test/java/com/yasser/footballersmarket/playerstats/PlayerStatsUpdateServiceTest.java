package com.yasser.footballersmarket.playerstats;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.fixture.FixtureResultConstants;
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

    private PlayerStatsUpdateService service;

    @BeforeEach
    void setUp() {
        service = new PlayerStatsUpdateService(fixtureRepository, playerRepository, leagueStatsRepository,
                championsLeagueStatsRepository, pendingPlayerRepository, tournamentCatalog);
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

    // ---- helpers ----

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
