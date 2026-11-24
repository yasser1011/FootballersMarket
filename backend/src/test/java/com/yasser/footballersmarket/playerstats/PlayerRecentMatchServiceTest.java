package com.yasser.footballersmarket.playerstats;

import com.yasser.footballersmarket.fixture.Team;
import com.yasser.footballersmarket.fixture.TeamRepository;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerRecentMatchServiceTest {
    private static final Long SAKA_ID = 1460L;
    private static final Long ARSENAL_ID = 42L;
    private static final Long CHELSEA_ID = 49L;
    private static final Long SPURS_ID = 47L;
    private static final Long BARCELONA_ID = 529L;

    @Mock
    private PlayerMatchRatingRepository matchRatingRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;

    private PlayerRecentMatchService service;

    @BeforeEach
    void setUp() {
        service = new PlayerRecentMatchService(matchRatingRepository, teamRepository, playerRepository);
    }

    @Test
    void shouldMapAFixturePlayersLastFiveNewestFirst() {
        givenRows(CHELSEA_ID, SPURS_ID, CHELSEA_ID, SPURS_ID, BARCELONA_ID);
        givenTeams(new Team(CHELSEA_ID, "Chelsea", "x"), new Team(SPURS_ID, "Tottenham", "x"),
                new Team(BARCELONA_ID, "Barcelona", "x"));

        List<PlayerRecentMatch> matches = service.fromFixtures(SAKA_ID, "fixture", 0).orElseThrow();

        assertThat(matches).extracting(PlayerRecentMatch::getOpponentTeamName)
                .containsExactly("Chelsea", "Tottenham", "Chelsea", "Tottenham", "Barcelona");
        assertThat(matches.get(0).getRating()).isEqualTo(8.0);
        // 2026-11-21T12:30Z -> the utc date, like the 365 path
        assertThat(matches.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    void shouldUseTheBadgeOfAClubWeMaintainAndLeaveTheOthersEmpty() {
        givenRows(CHELSEA_ID, SPURS_ID, CHELSEA_ID, SPURS_ID, BARCELONA_ID);
        givenTeams(new Team(CHELSEA_ID, "Chelsea", "x"), new Team(SPURS_ID, "Tottenham", "x"),
                new Team(BARCELONA_ID, "Barcelona", "x"));
        when(playerRepository.findMostCommonClubPhotoUrlOfTeam(CHELSEA_ID)).thenReturn("https://365/chelsea.png");
        when(playerRepository.findMostCommonClubPhotoUrlOfTeam(SPURS_ID)).thenReturn("https://365/spurs.png");
        // a cl opponent from a league we do not cover: no players of his, so no badge

        List<PlayerRecentMatch> matches = service.fromFixtures(SAKA_ID, "fixture", 0).orElseThrow();

        assertThat(matches).extracting(PlayerRecentMatch::getOpponentTeamPhotoUrl)
                .containsExactly("https://365/chelsea.png", "https://365/spurs.png", "https://365/chelsea.png",
                        "https://365/spurs.png", null);
        // each club looked up once, though chelsea and spurs appear twice
        verify(playerRepository, times(1)).findMostCommonClubPhotoUrlOfTeam(CHELSEA_ID);
        verify(playerRepository, times(1)).findMostCommonClubPhotoUrlOfTeam(SPURS_ID);
    }

    @Test
    void shouldFallBackWithFewerThanFiveMatchesOnRecord() {
        when(matchRatingRepository.findTop5ByPlayerIdOrderByFixtureDateDesc(SAKA_ID))
                .thenReturn(rows(CHELSEA_ID, SPURS_ID, CHELSEA_ID, SPURS_ID));

        assertThat(service.fromFixtures(SAKA_ID, "fixture", 0)).isEmpty();
        verifyNoInteractions(teamRepository, playerRepository);
    }

    @Test
    void shouldFallBackForAnExternallyMaintainedPlayerWithoutReadingTheTable() {
        assertThat(service.fromFixtures(SAKA_ID, "sofascore", 0)).isEmpty();
        assertThat(service.fromFixtures(SAKA_ID, "sofascore_buy", 0)).isEmpty();
        verifyNoInteractions(matchRatingRepository);
    }

    @Test
    void shouldFallBackForAFlaggedPlayer() {
        assertThat(service.fromFixtures(SAKA_ID, "fixture", 4)).isEmpty();
        verifyNoInteractions(matchRatingRepository);
    }

    @Test
    void shouldFallBackWhenAnOpponentHasNoTeamRowToNameIt() {
        givenRows(CHELSEA_ID, SPURS_ID, CHELSEA_ID, SPURS_ID, BARCELONA_ID);
        givenTeams(new Team(CHELSEA_ID, "Chelsea", "x"), new Team(SPURS_ID, "Tottenham", "x"));

        assertThat(service.fromFixtures(SAKA_ID, "fixture", 0)).isEmpty();
        verify(playerRepository, never()).findMostCommonClubPhotoUrlOfTeam(any());
    }

    // ---- helpers ----

    private void givenRows(Long... opponents) {
        when(matchRatingRepository.findTop5ByPlayerIdOrderByFixtureDateDesc(SAKA_ID)).thenReturn(rows(opponents));
    }

    private void givenTeams(Team... teams) {
        when(teamRepository.findAllById(any())).thenReturn(List.of(teams));
    }

    // newest first, as the repository returns them: 8.0 on 21 nov, then one point and a few days less each
    private List<PlayerMatchRating> rows(Long... opponents) {
        List<PlayerMatchRating> rows = new ArrayList<>();
        Instant newest = Instant.parse("2026-11-21T12:30:00Z");
        for (int i = 0; i < opponents.length; i++) {
            rows.add(new PlayerMatchRating(SAKA_ID, 1000L - i, newest.minusSeconds(i * 4L * 86400), 39L,
                    ARSENAL_ID, opponents[i], 90, 8.0 - i * 0.1, 0, 0));
        }
        return rows;
    }
}
