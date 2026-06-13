package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

// answers "is this player's team in a match that's being played right now". used by the buy
// flow to block buying a player while his match is live (price is mid-swing). only world cup
// players carry a stats row + team, so non-participants always read as not playing.
@Service
public class WorldCupLiveMatchService {
    private final PlayerWorldCupStatsRepository playerWorldCupStatsRepository;
    private final WorldCupFixtureRepository fixtureRepository;
    private final Logger logger = LoggerFactory.getLogger(WorldCupLiveMatchService.class);

    public WorldCupLiveMatchService(PlayerWorldCupStatsRepository playerWorldCupStatsRepository,
                                    WorldCupFixtureRepository fixtureRepository) {
        this.playerWorldCupStatsRepository = playerWorldCupStatsRepository;
        this.fixtureRepository = fixtureRepository;
    }

    public boolean isPlayerInLiveMatch(Long playerId) {
        if (playerId == null) return false;
        PlayerWorldCupStats stats = playerWorldCupStatsRepository.findById(playerId).orElse(null);
        if (stats == null || stats.getTeamId() == null) return false; // not a world cup player

        Instant now = Instant.now();
        List<WorldCupFixture> kickedOff = fixtureRepository.findKickedOffNotFinishedForTeam(
                stats.getTeamId(), WorldCupConstants.FINISHED_STATUSES, now);

        // a fixture is still "live" while now is within its kickoff + checkAgainMinutes window,
        // the same per-fixture window the results scheduler widens until the final result lands.
        boolean live = kickedOff.stream().anyMatch(f ->
                now.isBefore(f.getDate().plus(f.getCheckAgainMinutes(), ChronoUnit.MINUTES)));
        if (live) {
            logger.info("buy blocked: player {} (team {}) is in a live match", playerId, stats.getTeamId());
        }
        return live;
    }
}
