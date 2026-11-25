package com.yasser.footballersmarket.player;

import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.playerstats.PlayerRecentMatchService;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// the details page flow by player case: who is read from our db, who is refreshed from 365 and
// written, and when 365 is not called at all
@ExtendWith(MockitoExtension.class)
class PlayerServiceDetailsTest {
    private static final Integer EXTERNAL_ID = 65760;
    private static final Long PLAYER_ID = 1100L;

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerLeagueStatsService playerLeagueStatsService;
    @Mock
    private PriceStrategy priceStrategy;
    @Mock
    private ScoresService scoresService;
    @Mock
    private PlayerRecentMatchService recentMatchService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private PlayerService playerService;

    private final List<PlayerRecentMatch> ourLastFive = List.of(new PlayerRecentMatch("Chelsea", 8.1, null, LocalDate.of(2026, 11, 21)));
    private final List<PlayerRecentMatch> theirLastFive = List.of(new PlayerRecentMatch("Fulham", 6.9, 88, LocalDate.of(2026, 11, 21)));

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, playerLeagueStatsService, priceStrategy,
                scoresService, recentMatchService, transactionManager);
    }

    // ---- not in our db (a search result) ----

    @Test
    void shouldBuildAPlayerWeDoNotHaveFrom365WithoutSavingHim() {
        PlayerDetailsDto external = external("Al Hilal", 7.4);
        when(scoresService.fetchPlayerEntityFromExternalService(EXTERNAL_ID)).thenReturn(external);

        PlayerDetailsResDto details = playerService.getPlayerDetails(EXTERNAL_ID);

        assertThat(details.getId()).isNull();
        assertThat(details.getExternalServicePlayerClub()).isEqualTo("Al Hilal");
        assertThat(details.getLeagueStats().getRating()).isEqualTo(7.4);
        assertThat(details.getRecentMatches()).isEqualTo(theirLastFive);
        verify(playerLeagueStatsService, never()).savePlayerLeagueStats(any());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void shouldNotFindAPlayerWeDoNotHaveWhen365FailsToo() {
        assertThatThrownBy(() -> playerService.getPlayerDetails(EXTERNAL_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- fixture-maintained ----

    @Test
    void shouldNotCall365ForAFixturePlayerWithFiveMatchesOnRecord() {
        givenStored(player("fixture", 0));
        when(recentMatchService.fromFixtures(PLAYER_ID, "fixture", 0)).thenReturn(Optional.of(ourLastFive));

        PlayerDetailsResDto details = playerService.getPlayerDetails(EXTERNAL_ID);

        assertThat(details.getRecentMatches()).isEqualTo(ourLastFive);
        verify(scoresService, never()).fetchPlayerEntityFromExternalService(any());
        verify(scoresService, never()).fetchRecentMatches(any());
    }

    @Test
    void shouldAsk365OnlyForTheLastFiveOfAFixturePlayerWeLackThemFor() {
        givenStored(player("fixture", 0));
        when(scoresService.fetchRecentMatches(EXTERNAL_ID)).thenReturn(theirLastFive);

        PlayerDetailsResDto details = playerService.getPlayerDetails(EXTERNAL_ID);

        assertThat(details.getRecentMatches()).isEqualTo(theirLastFive);
        // his stats are ours: never refreshed or written from 365
        verify(scoresService, never()).fetchPlayerEntityFromExternalService(any());
        verify(playerLeagueStatsService, never()).savePlayerLeagueStats(any());
    }

    // ---- externally maintained: sofascore, sofascore_buy, flagged ----

    @Test
    void shouldRefreshAndWriteASofascorePlayer() {
        givenStored(player("sofascore", 0));
        givenExternal(external("Al Hilal", 7.9));

        PlayerDetailsResDto details = playerService.getPlayerDetails(EXTERNAL_ID);

        assertThat(details.getLeagueStats().getRating()).isEqualTo(7.9);
        assertThat(details.getRecentMatches()).isEqualTo(theirLastFive);
        verify(playerLeagueStatsService).savePlayerLeagueStats(any());
        // not flagged: his stored club stays
        verify(playerRepository, never()).findById(any());
    }

    @Test
    void shouldRefreshAndWriteASofascoreBuyPlayer() {
        givenStored(player("sofascore_buy", 0));
        givenExternal(external("Al Hilal", 7.9));

        playerService.getPlayerDetails(EXTERNAL_ID);

        verify(playerLeagueStatsService).savePlayerLeagueStats(any());
    }

    @Test
    void shouldFollowTheClub365ReportsForAFlaggedPlayer() {
        Player flagged = player("fixture", 4);
        givenStored(flagged);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(flagged));
        givenExternal(external("Al Hilal", 7.9));
        when(scoresService.clubPhotoUrl(77)).thenReturn("https://365/alhilal.png");

        PlayerDetailsResDto details = playerService.getPlayerDetails(EXTERNAL_ID);

        assertThat(flagged.getSofascoreClub()).isEqualTo("Al Hilal");
        assertThat(flagged.getClubPhotoUrl()).isEqualTo("https://365/alhilal.png");
        assertThat(details.getExternalServicePlayerClub()).isEqualTo("Al Hilal");
        assertThat(details.getClubPhotoUrl()).isEqualTo("https://365/alhilal.png");
        verify(playerLeagueStatsService).savePlayerLeagueStats(any());
    }

    @Test
    void shouldWriteTheRefreshInOneTransaction() {
        givenStored(player("fixture", 4));
        givenExternal(external("Al Hilal", 7.9));

        playerService.getPlayerDetails(EXTERNAL_ID);

        verify(transactionManager).getTransaction(any());
        verify(transactionManager).commit(any());
    }

    @Test
    void shouldShowTheStoredDataWhen365FailsForAnExternallyMaintainedPlayer() {
        givenStored(player("sofascore", 0));

        PlayerDetailsResDto details = playerService.getPlayerDetails(EXTERNAL_ID);

        assertThat(details.getId()).isEqualTo(PLAYER_ID);
        assertThat(details.getLeagueStats().getRating()).isEqualTo(7.0);
        verify(playerLeagueStatsService, never()).savePlayerLeagueStats(any());
    }

    // ---- last ratings alone ----

    @Test
    void shouldGiveTheLastFiveOfAPlayerWeDoNotHaveFrom365() {
        when(scoresService.fetchRecentMatches(EXTERNAL_ID)).thenReturn(theirLastFive);

        assertThat(playerService.getLastRatings(EXTERNAL_ID)).isEqualTo(theirLastFive);
    }

    @Test
    void shouldGiveTheLastFiveOfAFixturePlayerFromOurData() {
        givenStored(player("fixture", 0));
        when(recentMatchService.fromFixtures(PLAYER_ID, "fixture", 0)).thenReturn(Optional.of(ourLastFive));

        assertThat(playerService.getLastRatings(EXTERNAL_ID)).isEqualTo(ourLastFive);
        verify(scoresService, never()).fetchRecentMatches(any());
    }

    // ---- helpers ----

    private Player player(String updatedBy, int missedMatches) {
        Player player = new Player(PLAYER_ID, EXTERNAL_ID, "Player", LocalDate.of(2000, 1, 1),
                "Arsenal", "Arsenal", updatedBy);
        player.setLeagueStats(new PlayerLeagueStats(PLAYER_ID, 3, 1, 10, 7.0, player));
        player.setMissedMatches(missedMatches);
        return player;
    }

    private void givenStored(Player player) {
        when(playerRepository.findFirstBySofascoreId(EXTERNAL_ID)).thenReturn(player);
    }

    private void givenExternal(PlayerDetailsDto external) {
        when(scoresService.fetchPlayerEntityFromExternalService(EXTERNAL_ID)).thenReturn(external);
    }

    private PlayerDetailsDto external(String clubName, double rating) {
        PlayerDetailsDto external = mock(PlayerDetailsDto.class);
        com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats leagueStats =
                new com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats(12, 4, 2, rating);
        when(external.getClubName()).thenReturn(clubName);
        when(external.getClubId()).thenReturn(77);
        when(external.getLeagueStats()).thenReturn(leagueStats);
        when(external.getRecentMatches()).thenReturn(theirLastFive);
        // createLeagueStatsDto is the 365 -> our stats mapping; a fresh instance per call, like the real one
        when(scoresService.createLeagueStatsDto(leagueStats))
                .thenAnswer(invocation -> new PlayerLeagueStats(4, 2, 12, rating));
        return external;
    }
}
