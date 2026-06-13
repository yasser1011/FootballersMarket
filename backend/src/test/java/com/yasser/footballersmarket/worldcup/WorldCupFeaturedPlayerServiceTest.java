package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayerRepository;
import com.yasser.footballersmarket.worldcup.WorldCupFeaturedPlayerService.RatedWcPlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldCupFeaturedPlayerServiceTest {

    @Mock private FeaturedPlayerRepository repository;
    @InjectMocks private WorldCupFeaturedPlayerService service;
    @Captor private ArgumentCaptor<List<FeaturedPlayer>> savedCaptor;

    private static final String ROUND = "Group Stage - 1";
    private static final long TEAM = 16L;
    private static final LocalDate MATCH_DATE = LocalDate.of(2026, 6, 13);
    private static final LocalDate EARLIER = LocalDate.of(2026, 6, 11);

    private FeaturedPlayer existing(long playerId, String rating, int position, LocalDate date) {
        return FeaturedPlayer.worldCupRow("p" + playerId, (int) playerId, rating, (int) TEAM, date, position, ROUND);
    }

    private RatedWcPlayer rated(long playerId, String rating) {
        return new RatedWcPlayer(playerId, "p" + playerId, rating, TEAM);
    }

    private List<FeaturedPlayer> saved() {
        verify(repository).deleteByFixtureName(ROUND);
        verify(repository).saveAll(savedCaptor.capture());
        return savedCaptor.getValue();
    }

    @Test
    void emptyRoundTakesTop5ByRatingWithPositions() {
        when(repository.findByFixtureNameOrderByPositionAsc(ROUND)).thenReturn(List.of());

        service.updateRoundTopFive(ROUND, MATCH_DATE, List.of(
                rated(1, "9.0"), rated(2, "8.0"), rated(3, "7.0"),
                rated(4, "6.0"), rated(5, "5.0"), rated(6, "4.0")));

        List<FeaturedPlayer> rows = saved();
        assertThat(rows).hasSize(5);
        assertThat(rows).extracting(FeaturedPlayer::getPlayerId).containsExactly(1, 2, 3, 4, 5); // 6 dropped
        assertThat(rows).extracting(FeaturedPlayer::getPosition).containsExactly(1, 2, 3, 4, 5);
        assertThat(rows.get(0).getRating()).isEqualTo("9.0");
        assertThat(rows.get(0).getDate()).isEqualTo(MATCH_DATE);
    }

    @Test
    void returningPlayerUpgradesToBetterRatingAndDate() {
        when(repository.findByFixtureNameOrderByPositionAsc(ROUND))
                .thenReturn(List.of(existing(1, "7.0", 1, EARLIER)));

        service.updateRoundTopFive(ROUND, MATCH_DATE, List.of(rated(1, "8.0")));

        FeaturedPlayer row = saved().get(0);
        assertThat(row.getPlayerId()).isEqualTo(1);
        assertThat(row.getRating()).isEqualTo("8.0");
        assertThat(row.getDate()).isEqualTo(MATCH_DATE); // date moves to the better match
    }

    @Test
    void lowerRatingDoesNotDowngradeExisting() {
        when(repository.findByFixtureNameOrderByPositionAsc(ROUND))
                .thenReturn(List.of(existing(1, "8.0", 1, EARLIER)));

        service.updateRoundTopFive(ROUND, MATCH_DATE, List.of(rated(1, "6.0")));

        FeaturedPlayer row = saved().get(0);
        assertThat(row.getRating()).isEqualTo("8.0");
        assertThat(row.getDate()).isEqualTo(EARLIER); // unchanged
    }

    @Test
    void newHigherPlayerBumpsTheLowest() {
        when(repository.findByFixtureNameOrderByPositionAsc(ROUND)).thenReturn(List.of(
                existing(1, "8.0", 1, EARLIER), existing(2, "7.0", 2, EARLIER),
                existing(3, "6.0", 3, EARLIER), existing(4, "5.0", 4, EARLIER),
                existing(5, "4.0", 5, EARLIER)));

        service.updateRoundTopFive(ROUND, MATCH_DATE, List.of(rated(6, "6.5")));

        List<FeaturedPlayer> rows = saved();
        assertThat(rows).hasSize(5);
        assertThat(rows).extracting(FeaturedPlayer::getPlayerId).containsExactly(1, 2, 6, 3, 4); // 5 dropped
    }

    @Test
    void emptyRatedListIsNoOp() {
        service.updateRoundTopFive(ROUND, MATCH_DATE, List.of());

        verify(repository, never()).findByFixtureNameOrderByPositionAsc(any());
        verify(repository, never()).deleteByFixtureName(any());
        verify(repository, never()).saveAll(any());
    }
}
