package com.yasser.footballersmarket.player;

import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.playerstats.PlayerRecentMatchService;
import com.yasser.footballersmarket.pricing.PlayerPricingData;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    void shouldStampWhenAnExternalRefreshWasWritten() {
        givenStored(player("sofascore", 0));
        givenExternal(external("Al Hilal", 7.9));

        playerService.getPlayerDetails(EXTERNAL_ID);

        verify(playerRepository).markExternallyRefreshed(eq(PLAYER_ID), any());
    }

    @Test
    void shouldStampARefreshEvenWhen365HasNoLeagueStatsForHim() {
        givenStored(player("sofascore", 0));
        PlayerDetailsDto external = mock(PlayerDetailsDto.class);
        when(external.getRecentMatches()).thenReturn(theirLastFive);
        givenExternal(external);

        playerService.getPlayerDetails(EXTERNAL_ID);

        // asked and answered: a buy in the next 3 days does not ask again
        verify(playerRepository).markExternallyRefreshed(eq(PLAYER_ID), any());
        verify(playerLeagueStatsService, never()).savePlayerLeagueStats(any());
    }

    // ---- the player a buy/sell is priced against ----

    @Test
    void shouldPriceATransactionOfAFixturePlayerOnStoredDataWithoutHttp() {
        givenStoredById(player("fixture", 0));

        PlayerDetailsResDto resolved = playerService.resolveForTransaction(PLAYER_ID, EXTERNAL_ID);

        assertThat(resolved.getId()).isEqualTo(PLAYER_ID);
        verify(scoresService, never()).fetchPlayerEntityFromExternalService(any());
    }

    @Test
    void shouldNotRefreshAnExternalPlayerRefreshedInTheLastThreeDays() {
        givenStoredById(player("sofascore", 0, Instant.now().minus(Duration.ofDays(1))));

        playerService.resolveForTransaction(PLAYER_ID, EXTERNAL_ID);

        verify(scoresService, never()).fetchPlayerEntityFromExternalService(any());
    }

    @Test
    void shouldRefreshAStaleExternalPlayerBeforePricingTheTransaction() {
        // the page was left open for weeks and nobody opened him since
        givenStoredById(player("fixture", 4, Instant.now().minus(Duration.ofDays(20))));
        givenExternal(external("Al Hilal", 7.9));

        PlayerDetailsResDto resolved = playerService.resolveForTransaction(PLAYER_ID, EXTERNAL_ID);

        assertThat(resolved.getLeagueStats().getRating()).isEqualTo(7.9);
        verify(playerLeagueStatsService).savePlayerLeagueStats(any());
        verify(playerRepository).markExternallyRefreshed(eq(PLAYER_ID), any());
    }

    @Test
    void shouldRefreshAnExternalPlayerNeverRefreshedBeforeTheTransaction() {
        givenStoredById(player("sofascore_buy", 0, null));
        givenExternal(external("Al Hilal", 7.9));

        playerService.resolveForTransaction(PLAYER_ID, EXTERNAL_ID);

        verify(playerLeagueStatsService).savePlayerLeagueStats(any());
    }

    @Test
    void shouldAllowTheStalePriceWhen365FailsDuringATransaction() {
        givenStoredById(player("sofascore", 0, Instant.now().minus(Duration.ofDays(20))));

        PlayerDetailsResDto resolved = playerService.resolveForTransaction(PLAYER_ID, EXTERNAL_ID);

        assertThat(resolved.getLeagueStats().getRating()).isEqualTo(7.0);
        verify(playerLeagueStatsService, never()).savePlayerLeagueStats(any());
    }

    @Test
    void shouldFindByExternalIdWhenTheRequestHasNoInternalId() {
        // he was a search result on the page, but someone else bought (and so created) him since
        givenStored(player("sofascore_buy", 0, Instant.now()));

        PlayerDetailsResDto resolved = playerService.resolveForTransaction(null, EXTERNAL_ID);

        assertThat(resolved.getId()).isEqualTo(PLAYER_ID);
        verify(scoresService, never()).fetchPlayerEntityFromExternalService(any());
    }

    @Test
    void shouldPriceATransactionOfAPlayerWeDoNotHaveFrom365() {
        givenExternal(external("Al Hilal", 7.4));

        PlayerDetailsResDto resolved = playerService.resolveForTransaction(null, EXTERNAL_ID);

        assertThat(resolved.getId()).isNull();
        assertThat(resolved.getLeagueStats().getRating()).isEqualTo(7.4);
    }

    @Test
    void shouldRejectATransactionOfAPlayerNeitherWeNor365Have() {
        assertThatThrownBy(() -> playerService.resolveForTransaction(null, EXTERNAL_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- a player as the rankings and the squad list him ----

    @Test
    void shouldListAFixturePlayerAsStoredWithoutHttp() {
        PlayerDetailsResDto stored = playerService.convertPlayerEntityToPlayerDto(player("fixture", 0));

        PlayerDetailsResDto listed = playerService.refreshedForListing(stored);

        assertThat(listed.getLeagueStats().getRating()).isEqualTo(7.0);
        verify(scoresService, never()).fetchPlayerEntityFromExternalService(any());
    }

    @Test
    void shouldRefreshAndRepriceAnExternalPlayerOnHisStoredChampionsLeagueStats() {
        // the rankings used to rebuild him from 365 alone and drop this row from his price
        Player stored = player("sofascore", 0);
        stored.setChampionsLeagueStats(new PlayerChampionsLeagueStats(PLAYER_ID, 2, 0, 0, 7.5, stored));
        givenExternal(external("Arsenal", 7.9));

        PlayerDetailsResDto listed = playerService.refreshedForListing(playerService.convertPlayerEntityToPlayerDto(stored));

        assertThat(listed.getLeagueStats().getRating()).isEqualTo(7.9);
        verify(playerLeagueStatsService).savePlayerLeagueStats(any());
        // priced once when converted (7.0 / 7.5) and again after the refresh: 365's league + our CL
        ArgumentCaptor<PlayerPricingData> pricing = ArgumentCaptor.forClass(PlayerPricingData.class);
        verify(priceStrategy, times(2)).calculatePrice(pricing.capture());
        assertThat(pricing.getValue().leagueRating()).isEqualTo(7.7);
        assertThat(pricing.getValue().dateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    void shouldListAnExternalPlayerOnStoredDataWhen365Fails() {
        PlayerDetailsResDto stored = playerService.convertPlayerEntityToPlayerDto(player("sofascore", 0));

        PlayerDetailsResDto listed = playerService.refreshedForListing(stored);

        assertThat(listed.getLeagueStats().getRating()).isEqualTo(7.0);
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
        return player(updatedBy, missedMatches, null);
    }

    private Player player(String updatedBy, int missedMatches, Instant externallyRefreshedAt) {
        Player player = new Player(PLAYER_ID, EXTERNAL_ID, "Player", LocalDate.of(2000, 1, 1),
                "Arsenal", "Arsenal", updatedBy);
        player.setLeagueStats(new PlayerLeagueStats(PLAYER_ID, 3, 1, 10, 7.0, player));
        player.setMissedMatches(missedMatches);
        player.setExternallyRefreshedAt(externallyRefreshedAt);
        return player;
    }

    private void givenStored(Player player) {
        when(playerRepository.findFirstBySofascoreId(EXTERNAL_ID)).thenReturn(player);
    }

    private void givenStoredById(Player player) {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
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
        // lenient: a transaction's refresh has no page to show them on, so it never reads them
        lenient().when(external.getRecentMatches()).thenReturn(theirLastFive);
        // createLeagueStatsDto is the 365 -> our stats mapping; a fresh instance per call, like the real one
        when(scoresService.createLeagueStatsDto(leagueStats))
                .thenAnswer(invocation -> new PlayerLeagueStats(4, 2, 12, rating));
        return external;
    }
}
