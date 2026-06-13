package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStatsRepository;
import com.yasser.footballersmarket.worldcup.WorldCupFeaturedPlayerService.RatedWcPlayer;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.FixtureResult;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.PlayerEntry;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Statistics;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.TeamPlayers;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.Teams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

// applies a fetched fixture result: records the score/winner, updates each tracked player's
// world cup stats from his match rating, and settles predictions. the fixture status flip
// (not-finished -> finished) is the once-only latch — finished fixtures are never re-polled,
// so player stats are applied exactly once and settlement is independently idempotent.
@Service
public class WorldCupResultsService {
    private final WorldCupFixtureRepository fixtureRepository;
    private final PlayerWorldCupStatsRepository playerWorldCupStatsRepository;
    private final WorldCupSettlementService settlementService;
    private final WorldCupFeaturedPlayerService featuredPlayerService;
    private final Logger logger = LoggerFactory.getLogger(WorldCupResultsService.class);

    // match date for featured rows is taken in the market timezone (matches the rest of the WC feature)
    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    public WorldCupResultsService(WorldCupFixtureRepository fixtureRepository,
                                  PlayerWorldCupStatsRepository playerWorldCupStatsRepository,
                                  WorldCupSettlementService settlementService,
                                  WorldCupFeaturedPlayerService featuredPlayerService) {
        this.fixtureRepository = fixtureRepository;
        this.playerWorldCupStatsRepository = playerWorldCupStatsRepository;
        this.settlementService = settlementService;
        this.featuredPlayerService = featuredPlayerService;
    }

    @Transactional
    public void processFixtureResult(WorldCupFixture fixture, FixtureResult result) {
        String shortCode = result.fixture() != null && result.fixture().status() != null
                ? result.fixture().status().shortCode() : null;

        if (!WorldCupConstants.FINISHED_STATUSES.contains(shortCode)) {
            // still upcoming or in play: poll again later, widening the gap each miss
            fixture.setCheckAgainMinutes(fixture.getCheckAgainMinutes() + WorldCupConstants.CHECK_AGAIN_BACKOFF_MINUTES);
            fixtureRepository.save(fixture);
            logger.info("world cup results: fixture {} not finished (status {}), next check +{} min total {}",
                    fixture.getId(), shortCode, WorldCupConstants.CHECK_AGAIN_BACKOFF_MINUTES, fixture.getCheckAgainMinutes());
            return;
        }

        recordResult(fixture, shortCode, result);
        List<RatedWcPlayer> ratedPlayers = applyPlayerStats(result);
        fixtureRepository.save(fixture);
        settlementService.settleFixture(fixture);
        // feed the round's running top-5 featured players from this match's ratings
        LocalDate matchDate = LocalDate.ofInstant(fixture.getDate(), ZONE);
        featuredPlayerService.updateRoundTopFive(fixture.getRound(), matchDate, ratedPlayers);
        logger.info("world cup results: fixture {} finished {}-{}, winner team {}",
                fixture.getId(), fixture.getHomeGoals(), fixture.getAwayGoals(), fixture.getWinnerTeamId());
    }

    private void recordResult(WorldCupFixture fixture, String shortCode, FixtureResult result) {
        if (result.goals() != null) {
            fixture.setHomeGoals(result.goals().home());
            fixture.setAwayGoals(result.goals().away());
        }
        fixture.setWinnerTeamId(resolveWinner(fixture, result.teams()));
        fixture.setStatus(shortCode);
    }

    // the api winner flag is who advanced (true incl. penalty-shootout wins); both null/false = draw
    private Long resolveWinner(WorldCupFixture fixture, Teams teams) {
        if (teams == null) return null;
        if (teams.home() != null && Boolean.TRUE.equals(teams.home().winner())) return fixture.getHomeTeamId();
        if (teams.away() != null && Boolean.TRUE.equals(teams.away().winner())) return fixture.getAwayTeamId();
        return null;
    }

    // updates cumulative stats and returns the tracked players who actually played, with their
    // match rating, for the featured-players running top-5
    private List<RatedWcPlayer> applyPlayerStats(FixtureResult result) {
        List<RatedWcPlayer> rated = new ArrayList<>();
        if (result.players() == null) return rated;
        for (TeamPlayers teamPlayers : result.players()) {
            if (teamPlayers.players() == null) continue;
            for (PlayerEntry entry : teamPlayers.players()) {
                RatedWcPlayer ratedPlayer = applyOnePlayer(entry);
                if (ratedPlayer != null) rated.add(ratedPlayer);
            }
        }
        return rated;
    }

    private RatedWcPlayer applyOnePlayer(PlayerEntry entry) {
        if (entry.player() == null || entry.player().id() == null
                || entry.statistics() == null || entry.statistics().isEmpty()) {
            return null;
        }
        Statistics stat = entry.statistics().get(0);
        String ratingStr = stat.games() != null ? stat.games().rating() : null;
        Double matchRating = parseRating(ratingStr);
        if (matchRating == null) return null; // unused sub / no rating -> did not play, skip

        // only players in our seeded world cup squads carry a stats row; ignore everyone else
        PlayerWorldCupStats stats = playerWorldCupStatsRepository.findById(entry.player().id()).orElse(null);
        if (stats == null) return null;

        int games = nullSafe(stats.getTotalNumOfGames());
        // running average of match ratings; first game seeds it directly (no 6.5 anchor blended in)
        double newRating = games == 0 ? matchRating : (stats.getRating() * games + matchRating) / (games + 1);
        stats.setRating(newRating);
        stats.setTotalNumOfGames(games + 1);
        stats.setGoals(nullSafe(stats.getGoals()) + matchGoals(stat));
        stats.setAssists(nullSafe(stats.getAssists()) + matchAssists(stat));
        playerWorldCupStatsRepository.save(stats);

        return new RatedWcPlayer(entry.player().id(), entry.player().name(), ratingStr, stats.getTeamId());
    }

    private Double parseRating(String rating) {
        if (rating == null || rating.isBlank()) return null;
        try {
            return Double.parseDouble(rating);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int matchGoals(Statistics stat) {
        return stat.goals() == null ? 0 : nullSafe(stat.goals().total());
    }

    private int matchAssists(Statistics stat) {
        return stat.goals() == null ? 0 : nullSafe(stat.goals().assists());
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
