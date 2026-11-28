package com.yasser.footballersmarket.featuredplayer;

import com.yasser.footballersmarket.fixture.Team;
import com.yasser.footballersmarket.fixture.TeamRepository;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.playerstats.PlayerMatchRatingRepository;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.worldcup.WorldCupTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// turning the query's top 5 into the card's rows (which rows the query picks is
// FeaturedPlayerIntegrationTest's, it needs postgres)
@ExtendWith(MockitoExtension.class)
class FeaturedPlayerServiceTest {
    @Mock
    private FeaturedPlayerRepository featuredPlayerRepository;
    @Mock
    private PlayerMatchRatingRepository playerMatchRatingRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private WorldCupTeamRepository worldCupTeamRepository;

    private FeaturedPlayerService featuredPlayerService;

    @BeforeEach
    void setUp() {
        featuredPlayerService = new FeaturedPlayerService(featuredPlayerRepository, playerMatchRatingRepository,
                playerRepository, teamRepository, worldCupTeamRepository);
    }

    @Test
    void shouldReturnAnEmptyListWhenNoRoundHasBeenApplied() {
        when(playerMatchRatingRepository.findTop5OfCurrentRounds(TournamentCatalog.CHAMPIONS_LEAGUE_ID)).thenReturn(List.of());

        FeaturedPlayersResponse response = featuredPlayerService.getFeaturedPlayers();

        assertThat(response.getFeaturedPlayers()).isEmpty();
        verify(playerRepository, never()).findAllById(any());
    }

    @Test
    void shouldBuildTheCardRowsInTheQuerysOrder() {
        when(playerMatchRatingRepository.findTop5OfCurrentRounds(TournamentCatalog.CHAMPIONS_LEAGUE_ID)).thenReturn(List.of(
                row(1L, 9.1, 90, 40L, 50L),
                row(2L, 8.7, 64, 60L, 70L)));
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(1L, "Saka", 700), player(2L, "Vinicius", 800)));
        when(teamRepository.findAllById(any())).thenReturn(List.of(
                new Team(40L, "Arsenal", null), new Team(50L, "Chelsea", null),
                new Team(60L, "Real Madrid", null), new Team(70L, "Liverpool", null)));

        List<FeaturedPlayer> rows = featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers();

        assertThat(rows).extracting(FeaturedPlayer::getPlayerName).containsExactly("Saka", "Vinicius");
        assertThat(rows).extracting(FeaturedPlayer::getPosition).containsExactly(1, 2);
        FeaturedPlayer first = rows.get(0);
        assertThat(first.getRating()).isEqualTo("9.1");
        assertThat(first.getPlayerId()).isEqualTo(700);          // his 365 id, as before
        assertThat(first.getHomeTeamName()).isEqualTo("Arsenal");
        assertThat(first.getAwayTeamName()).isEqualTo("Chelsea");
        assertThat(first.getPlayerImgUrl()).isEqualTo("photo-1");
        assertThat(first.getPlayerTeamImgUrl()).isEqualTo("badge-1");
    }

    @Test
    void shouldShowARatingWithOneDecimal() {
        when(playerMatchRatingRepository.findTop5OfCurrentRounds(TournamentCatalog.CHAMPIONS_LEAGUE_ID)).thenReturn(List.<Object[]>of(row(1L, 7.0, 90, 40L, 50L)));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(1L, "Saka", 700)));

        assertThat(featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers().get(0).getRating()).isEqualTo("7.0");
    }

    @Test
    void shouldSkipAPlayerDeletedSinceAndKeepThePositionsGapless() {
        when(playerMatchRatingRepository.findTop5OfCurrentRounds(TournamentCatalog.CHAMPIONS_LEAGUE_ID)).thenReturn(List.of(
                row(1L, 9.1, 90, 40L, 50L),
                row(2L, 8.7, 64, 60L, 70L)));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(2L, "Vinicius", 800)));

        List<FeaturedPlayer> rows = featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers();

        assertThat(rows).extracting(FeaturedPlayer::getPlayerName).containsExactly("Vinicius");
        assertThat(rows.get(0).getPosition()).isEqualTo(1);
    }

    // the native query's row: bigint ids come back as BigInteger
    private Object[] row(Long playerId, double rating, int minutes, Long homeTeamId, Long awayTeamId) {
        return new Object[]{BigInteger.valueOf(playerId), rating, minutes,
                BigInteger.valueOf(homeTeamId), BigInteger.valueOf(awayTeamId)};
    }

    private Player player(Long id, String name, Integer externalId) {
        Player player = new Player(id, externalId, name, null, "Club", "Club", "fixture");
        player.setPhotoUrl("photo-" + id);
        player.setClubPhotoUrl("badge-" + id);
        return player;
    }
}
