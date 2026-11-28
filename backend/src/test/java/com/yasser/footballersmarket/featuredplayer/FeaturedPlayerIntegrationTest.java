package com.yasser.footballersmarket.featuredplayer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.fixture.Team;
import com.yasser.footballersmarket.fixture.TeamRepository;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.playerstats.PlayerMatchRating;
import com.yasser.footballersmarket.playerstats.PlayerMatchRatingRepository;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// which ratings the featured players query picks, against postgres: the round each competition is
// on (CL included), switching on its first applied match, best 5 overall, ties on minutes
@AutoConfigureMockMvc
class FeaturedPlayerIntegrationTest extends BaseIntegrationTest {
    private static final long PL = 39L;
    private static final long CL = 2L;
    private static final long ARSENAL = 9001L, CHELSEA = 9002L, LIVERPOOL = 9003L, SPURS = 9004L,
            REAL = 9005L, BAYERN = 9006L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FeaturedPlayerService featuredPlayerService;
    @Autowired
    private PlayerMatchRatingRepository playerMatchRatingRepository;
    @Autowired
    private FixtureRepository fixtureRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private PlayerRepository playerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final List<Long> fixtureIds = new ArrayList<>();
    private final List<Long> playerIds = new ArrayList<>();
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void setUp() {
        cleanUp();
        teamRepository.saveAll(List.of(new Team(ARSENAL, "Arsenal", null), new Team(CHELSEA, "Chelsea", null),
                new Team(LIVERPOOL, "Liverpool", null), new Team(SPURS, "Spurs", null),
                new Team(REAL, "Real Madrid", null), new Team(BAYERN, "Bayern", null)));
    }

    @AfterEach
    void cleanUp() {
        playerMatchRatingRepository.deleteAll();
        fixtureRepository.deleteAll(fixtureRepository.findAllById(fixtureIds));
        playerRepository.deleteAll(playerRepository.findAllById(playerIds));
        teamRepository.deleteAll(teamRepository.findAllById(List.of(ARSENAL, CHELSEA, LIVERPOOL, SPURS, REAL, BAYERN)));
    }

    @Test
    void shouldReturnAnEmptyListWhenNoRoundHasBeenApplied() {
        assertThat(featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers()).isEmpty();
    }

    @Test
    void shouldTakeTheBestRatingsOfTheLeaguesCurrentRoundsWhenALeagueMatchIsTheNewest() {
        // PL round 13 is over: even a 9.9 there no longer counts
        long round13 = fixture(91001L, PL, "Regular Season - 13", 8, true, ARSENAL, SPURS);
        rating(1L, round13, 9.9, 90);
        // PL round 14 has started (its first match applied), so the PL is on round 14
        long round14 = fixture(91002L, PL, "Regular Season - 14", 1, true, ARSENAL, CHELSEA);
        rating(2L, round14, 8.1, 90);
        rating(3L, round14, 7.2, 90);
        // a round 15 match not applied yet does not switch the round
        long round15 = fixture(91003L, PL, "Regular Season - 15", 0, false, LIVERPOOL, SPURS);
        rating(4L, round15, 9.8, 90);
        // the CL matchday came before the PL's latest match: it is out, however good
        long clMatchday5 = fixture(91004L, CL, "League Stage - 5", 3, true, REAL, BAYERN);
        rating(5L, clMatchday5, 9.5, 90);

        List<FeaturedPlayer> rows = featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers();

        assertThat(rows).extracting(FeaturedPlayer::getPlayerName).containsExactly("p2", "p3");
        assertThat(rows.get(0).getHomeTeamName()).isEqualTo("Arsenal");
        assertThat(rows.get(0).getAwayTeamName()).isEqualTo("Chelsea");
        assertThat(rows.get(0).getRating()).isEqualTo("8.1");
    }

    @Test
    void shouldTakeTheChampionsLeagueRoundAloneWhileItIsTheNewest() {
        long round14 = fixture(91007L, PL, "Regular Season - 14", 3, true, ARSENAL, CHELSEA);
        rating(1L, round14, 9.6, 90);
        long clMatchday5 = fixture(91008L, CL, "League Stage - 5", 1, true, REAL, BAYERN);
        rating(2L, clMatchday5, 8.4, 90);
        rating(3L, clMatchday5, 7.9, 90);

        List<FeaturedPlayer> rows = featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers();

        // the PL's 9.6 waits: the CL matchday is the newest thing played
        assertThat(rows).extracting(FeaturedPlayer::getPlayerName).containsExactly("p2", "p3");
        assertThat(rows.get(0).getHomeTeamName()).isEqualTo("Real Madrid");
    }

    @Test
    void shouldTakeTheChampionsLeagueRoundWhenNoLeagueMatchHasBeenAppliedYet() {
        long clMatchday1 = fixture(91009L, CL, "League Stage - 1", 1, true, REAL, BAYERN);
        rating(1L, clMatchday1, 8.0, 90);

        assertThat(featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers())
                .extracting(FeaturedPlayer::getPlayerName).containsExactly("p1");
    }

    @Test
    void shouldKeepTheBestFiveAndBreakTiesOnMinutes() {
        long match = fixture(91005L, PL, "Regular Season - 14", 1, true, ARSENAL, CHELSEA);
        rating(1L, match, 7.0, 90);
        rating(2L, match, 8.0, 45);
        rating(3L, match, 8.0, 90);   // same rating, more minutes -> ahead of p2
        rating(4L, match, 7.5, 90);
        rating(5L, match, 6.9, 90);
        rating(6L, match, 6.0, 90);   // 6th: left out

        List<FeaturedPlayer> rows = featuredPlayerService.getFeaturedPlayers().getFeaturedPlayers();

        assertThat(rows).extracting(FeaturedPlayer::getPlayerName).containsExactly("p3", "p2", "p4", "p1", "p5");
    }

    @Test
    void shouldServeTheCardRowsOverTheEndpoint() throws Exception {
        long match = fixture(91006L, PL, "Regular Season - 14", 1, true, ARSENAL, CHELSEA);
        rating(1L, match, 8.0, 90);

        MvcResult result = mockMvc.perform(get("/api/featured-players"))
                .andExpect(status().isOk())
                .andReturn();
        FeaturedPlayersResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(response.getFeaturedPlayers()).hasSize(1);
        FeaturedPlayer row = response.getFeaturedPlayers().get(0);
        assertThat(row.getPlayerName()).isEqualTo("p1");
        assertThat(row.getPlayerImgUrl()).isEqualTo("photo-1");
        assertThat(row.getPlayerTeamImgUrl()).isEqualTo("badge-1");
    }

    // ---- helpers ----

    private long fixture(long id, long leagueId, String round, int daysAgo, boolean applied, long home, long away) {
        Fixture fixture = new Fixture(id, now.minus(daysAgo, ChronoUnit.DAYS), leagueId, round,
                applied ? "FT" : "NS", home, away, null, null, null);
        fixture.setStatsApplied(applied);
        fixtureRepository.save(fixture);
        fixtureIds.add(id);
        return id;
    }

    // a rating for player p<n> (created on first use) in that fixture
    private void rating(long playerId, long fixtureId, double rating, int minutes) {
        Fixture fixture = fixtureRepository.findById(fixtureId).orElseThrow();
        if (!playerIds.contains(playerId)) {
            Player player = new Player(playerId, (int) (700 + playerId), "p" + playerId, null, "Club", "Club", "fixture");
            player.setPhotoUrl("photo-" + playerId);
            player.setClubPhotoUrl("badge-" + playerId);
            playerRepository.save(player);
            playerIds.add(playerId);
        }
        playerMatchRatingRepository.save(new PlayerMatchRating(playerId, fixtureId, fixture.getDate(),
                fixture.getLeagueId(), fixture.getHomeTeamId(), fixture.getAwayTeamId(), minutes, rating, 0, 0));
    }
}
