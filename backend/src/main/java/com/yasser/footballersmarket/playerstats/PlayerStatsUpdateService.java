package com.yasser.footballersmarket.playerstats;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureApiMapping;
import com.yasser.footballersmarket.fixture.FixtureFinishedHandler;
import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.Games;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response.StatGoals;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// adds a finished fixture's per-player minutes, ratings, goals and assists to the stats of every
// player we know who played in it. the fixtures are now the only thing that maintains these stats
// (the old rapid scheduler is off for good), so a match missed here is missed for good.
@Service
public class PlayerStatsUpdateService implements FixtureFinishedHandler {

    // updatedBy for a player whose stats come from fixture processing. anything not containing
    // "sofascore" already counts as rapid/fixture data in the existing queries and refresh checks
    static final String FIXTURE_MAINTAINED = "fixture";
    // externally maintained: the old scheduler skipped these because their club was out of scope
    private static final String SOFASCORE = "sofascore";
    // bought from an external search before we knew him: his id is a random placeholder, not a rapid id
    private static final String SOFASCORE_BUY = "sofascore_buy";

    private final FixtureRepository fixtureRepository;
    private final PlayerRepository playerRepository;
    private final PlayerLeagueStatsRepository leagueStatsRepository;
    private final PlayerChampionsLeagueStatsRepository championsLeagueStatsRepository;
    private final PendingPlayerRepository pendingPlayerRepository;
    private final TournamentCatalog tournamentCatalog;
    private final Logger logger = LoggerFactory.getLogger(PlayerStatsUpdateService.class);

    public PlayerStatsUpdateService(FixtureRepository fixtureRepository,
                                    PlayerRepository playerRepository,
                                    PlayerLeagueStatsRepository leagueStatsRepository,
                                    PlayerChampionsLeagueStatsRepository championsLeagueStatsRepository,
                                    PendingPlayerRepository pendingPlayerRepository,
                                    TournamentCatalog tournamentCatalog) {
        this.fixtureRepository = fixtureRepository;
        this.playerRepository = playerRepository;
        this.leagueStatsRepository = leagueStatsRepository;
        this.championsLeagueStatsRepository = championsLeagueStatsRepository;
        this.pendingPlayerRepository = pendingPlayerRepository;
        this.tournamentCatalog = tournamentCatalog;
    }

    // one player's line in the lineup, flattened out of the api's nested blocks
    record Appearance(Long playerId, String name, Long teamId, Games games, StatGoals goals) {}

    // plain @Transactional: applyResult has no transaction of its own, so this always starts a fresh
    // one and a failure here rolls back only these stats -- the fixture's result still stands. if a
    // transaction is ever added to the caller, this must become REQUIRES_NEW (see FixtureFinishedHandler).
    //
    // the whole fixture is one transaction, and statsApplied is flipped inside it: either every
    // player of this match is updated and the fixture is latched, or nothing is. a second call for
    // the same fixture then sees the latch and does nothing -- the running totals below are not
    // idempotent, so without it a re-run would count the match twice
    @Override
    @Transactional
    public void onFixtureFinished(Fixture finished, FixtureResponseDto.Response result) {
        // re-read: the latch must be checked against the stored row, not the caller's copy
        Fixture fixture = fixtureRepository.findById(finished.getId()).orElse(null);
        if (fixture == null) {
            logger.warn("player stats: fixture {} not found, skipping", finished.getId());
            return;
        }
        if (fixture.isStatsApplied()) {
            logger.info("player stats: fixture {} already applied, skipping", fixture.getId());
            return;
        }

        List<Appearance> lineup = lineupOf(result);
        // the api can mark a match finished before its player block is filled in. not latched, so
        // the fixture stays recognisable as "finished but not applied" for a later retry
        if (lineup.isEmpty()) {
            logger.warn("player stats: fixture {} finished without player data, not applied", fixture.getId());
            return;
        }

        boolean championsLeague = tournamentCatalog.isChampionsLeague(fixture.getLeagueId());
        Map<Long, Player> known = playerRepository.findAllById(
                        lineup.stream().map(Appearance::playerId).toList())
                .stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        List<PlayerLeagueStats> leagueRows = new ArrayList<>();
        List<PlayerChampionsLeagueStats> championsLeagueRows = new ArrayList<>();
        List<Player> backInScope = new ArrayList<>();
        List<Appearance> unknown = new ArrayList<>();

        for (Appearance appearance : lineup) {
            Player player = known.get(appearance.playerId());
            if (player == null) {
                unknown.add(appearance);
                continue;
            }
            // a placeholder id is random, so a match against a rapid id would be a coincidence --
            // never update him from a fixture. linking him to his real id is the pending-player job's
            if (SOFASCORE_BUY.equals(player.getUpdatedBy())) continue;

            // in a squad of a club we cover again, so fixtures maintain him from here on and the
            // external refresh stops writing over him
            if (SOFASCORE.equals(player.getUpdatedBy())) {
                player.setUpdatedBy(FIXTURE_MAINTAINED);
                backInScope.add(player);
            }

            // an unused sub was in the squad but did not play: no game to count
            if (!FixtureApiMapping.appeared(appearance.games())) continue;

            if (championsLeague) {
                championsLeagueRows.add(addToChampionsLeagueStats(player, appearance));
            } else {
                leagueRows.add(addToLeagueStats(player, appearance));
            }
        }

        if (!leagueRows.isEmpty()) leagueStatsRepository.saveAll(leagueRows);
        if (!championsLeagueRows.isEmpty()) championsLeagueStatsRepository.saveAll(championsLeagueRows);
        if (!backInScope.isEmpty()) playerRepository.saveAll(backInScope);

        // a champions league opponent from a league we do not track would never get league stats,
        // so only league fixtures put unknown players forward to be created
        int queued = championsLeague ? 0 : queueUnknownPlayers(unknown, fixture);

        fixture.setStatsApplied(true);
        fixtureRepository.save(fixture);

        logger.info("player stats: fixture {} applied -- {} player(s) updated, {} unknown ({} newly queued)",
                fixture.getId(), leagueRows.size() + championsLeagueRows.size(), unknown.size(), queued);
    }

    private PlayerLeagueStats addToLeagueStats(Player player, Appearance appearance) {
        PlayerLeagueStats row = player.getLeagueStats();
        // first league appearance on record (e.g. an academy debut): this match starts his row
        if (row == null) {
            row = new PlayerLeagueStats(player.getId(), 0, 0, 0, null, player);
            player.setLeagueStats(row);
        }
        int games = orZero(row.getTotalNumOfGames());
        row.setRating(runningAverage(row.getRating(), games, FixtureApiMapping.matchRating(appearance.games())));
        row.setTotalNumOfGames(games + 1);
        row.setGoals(orZero(row.getGoals()) + goals(appearance));
        row.setAssists(orZero(row.getAssists()) + assists(appearance));
        return row;
    }

    private PlayerChampionsLeagueStats addToChampionsLeagueStats(Player player, Appearance appearance) {
        PlayerChampionsLeagueStats row = player.getChampionsLeagueStats();
        // no row means no CL games so far (the seed stored one wherever the api had CL stats)
        if (row == null) {
            row = new PlayerChampionsLeagueStats(player.getId(), 0, 0, 0, null, player);
            player.setChampionsLeagueStats(row);
        }
        int games = orZero(row.getTotalNumOfGames());
        row.setRating(runningAverage(row.getRating(), games, FixtureApiMapping.matchRating(appearance.games())));
        row.setTotalNumOfGames(games + 1);
        row.setGoals(orZero(row.getGoals()) + goals(appearance));
        row.setAssists(orZero(row.getAssists()) + assists(appearance));
        return row;
    }

    // the mean over all games so far, folded one match at a time
    static double runningAverage(Double rating, int games, double matchRating) {
        if (games == 0 || rating == null) return matchRating;
        return (rating * games + matchRating) / (games + 1);
    }

    // insert-if-absent: a player seen in several fixtures before the job creates him is still one row
    private int queueUnknownPlayers(List<Appearance> unknown, Fixture fixture) {
        if (unknown.isEmpty()) return 0;

        Set<Long> alreadyPending = pendingPlayerRepository.findAllById(
                        unknown.stream().map(Appearance::playerId).toList())
                .stream()
                .map(PendingPlayer::getId)
                .collect(Collectors.toSet());

        Instant now = Instant.now();
        List<PendingPlayer> toQueue = unknown.stream()
                .filter(appearance -> !alreadyPending.contains(appearance.playerId()))
                .map(appearance -> new PendingPlayer(appearance.playerId(), appearance.name(),
                        appearance.teamId(), fixture.getId(), now))
                .toList();

        if (!toQueue.isEmpty()) pendingPlayerRepository.saveAll(toQueue);
        return toQueue.size();
    }

    // every squad member of both teams, unused subs included (they matter for the unknown-player
    // queue). an entry with no player id is unusable and dropped
    private List<Appearance> lineupOf(FixtureResponseDto.Response result) {
        List<Appearance> lineup = new ArrayList<>();
        if (result == null || result.players() == null) return lineup;

        for (FixtureResponseDto.Response.TeamPlayers block : result.players()) {
            if (block == null || block.players() == null) continue;
            Long teamId = block.team() != null ? block.team().id() : null;

            for (FixtureResponseDto.Response.PlayerEntry entry : block.players()) {
                if (entry == null || entry.player() == null || entry.player().id() == null) continue;
                // one statistics element per fixture; absent = nothing recorded for him
                var stats = entry.statistics() != null && !entry.statistics().isEmpty()
                        ? entry.statistics().get(0)
                        : null;
                lineup.add(new Appearance(entry.player().id(), entry.player().name(), teamId,
                        stats != null ? stats.games() : null,
                        stats != null ? stats.goals() : null));
            }
        }
        return lineup;
    }

    private static int goals(Appearance appearance) {
        return appearance.goals() != null ? orZero(appearance.goals().total()) : 0;
    }

    private static int assists(Appearance appearance) {
        return appearance.goals() != null ? orZero(appearance.goals().assists()) : 0;
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }
}
