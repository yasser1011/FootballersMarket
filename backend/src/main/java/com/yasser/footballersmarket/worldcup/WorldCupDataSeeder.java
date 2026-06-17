package com.yasser.footballersmarket.worldcup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStatsRepository;
import com.yasser.footballersmarket.scores365.ScoresConfig;
import me.xuender.unidecode.Unidecode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// seeds world cup teams and group stage fixtures from the json files prepared
// by scripts/worldcup-data-prep.ps1. only active with worldcup.seed=true and
// safe to rerun: ids are assigned from the seed data so saves act as upserts
@Component
@ConditionalOnProperty(name = "worldcup.seed", havingValue = "true")
public class WorldCupDataSeeder implements CommandLineRunner {
    private final WorldCupTeamRepository worldCupTeamRepository;
    private final WorldCupFixtureRepository worldCupFixtureRepository;
    private final PlayerRepository playerRepository;
    private final PlayerWorldCupStatsRepository playerWorldCupStatsRepository;
    private final ObjectMapper objectMapper;
    private final ScoresConfig scoresConfig;

    private final Logger logger = LoggerFactory.getLogger(WorldCupDataSeeder.class);

    public WorldCupDataSeeder(WorldCupTeamRepository worldCupTeamRepository,
                              WorldCupFixtureRepository worldCupFixtureRepository,
                              PlayerRepository playerRepository,
                              PlayerWorldCupStatsRepository playerWorldCupStatsRepository,
                              ObjectMapper objectMapper,
                              ScoresConfig scoresConfig) {
        this.worldCupTeamRepository = worldCupTeamRepository;
        this.worldCupFixtureRepository = worldCupFixtureRepository;
        this.playerRepository = playerRepository;
        this.playerWorldCupStatsRepository = playerWorldCupStatsRepository;
        this.objectMapper = objectMapper;
        this.scoresConfig = scoresConfig;
    }

    // seed json carries extra matching fields (code, scores365Id...) we don't persist
    record SeedTeam(Long id, String name, String code, String logoUrl, Integer scores365Id,
                    String scores365Name, Integer fifaRank, String groupName) {}

    record SeedFixture(Long id, String date, String round, String venueName, String venueCity,
                       Long homeTeamId, Long awayTeamId, String status) {}

    record SeedPlayer(Long rapidId, Integer scores365Id, String name, String rapidName, Integer jerseyNum,
                      String position, String dateOfBirth, Long teamId, String teamName, Integer clubId365,
                      String matchedVia) {}

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        seed();
    }

    public void seed() throws IOException {
        List<SeedTeam> seedTeams = objectMapper.readValue(
                new ClassPathResource("worldcup/teams.json").getInputStream(),
                new TypeReference<>() {});

        List<WorldCupTeam> teams = seedTeams.stream()
                .map(t -> new WorldCupTeam(t.id(), Unidecode.decode(t.name()), t.fifaRank(),
                        scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresClubsImageEndPoint() + t.scores365Id()))
                .collect(Collectors.toList());
        worldCupTeamRepository.saveAll(teams);
        logger.info("world cup seed: saved {} teams", teams.size());

        List<SeedFixture> seedFixtures = objectMapper.readValue(
                new ClassPathResource("worldcup/fixtures.json").getInputStream(),
                new TypeReference<>() {});

        List<WorldCupFixture> fixtures = seedFixtures.stream()
                .map(f -> new WorldCupFixture(f.id(), OffsetDateTime.parse(f.date()).toInstant(),
                        f.round(), f.homeTeamId(), f.awayTeamId(), f.status()))
                .collect(Collectors.toList());
        worldCupFixtureRepository.saveAll(fixtures);
        logger.info("world cup seed: saved {} fixtures", fixtures.size());

        seedPlayers(seedTeams);
    }

    // must run inside one transaction: the stats rows reference the freshly saved
    // players, and with assigned ids hibernate re-inserts detached parents otherwise
    private void seedPlayers(List<SeedTeam> seedTeams) throws IOException {
        List<SeedPlayer> seedPlayers = objectMapper.readValue(
                new ClassPathResource("worldcup/players.json").getInputStream(),
                new TypeReference<>() {});

        Map<Long, Integer> teamScores365Ids = seedTeams.stream()
                .collect(Collectors.toMap(SeedTeam::id, SeedTeam::scores365Id));

        // resolve players already in the player table: by rapid id (scheduler players)
        // or by sofascore id (players bought from search carry generated ids)
        Map<Long, Player> byId = playerRepository.findAllById(
                        seedPlayers.stream().map(SeedPlayer::rapidId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Player::getId, p -> p));
        Map<Integer, Player> bySofascoreId = playerRepository.findBySofascoreIdIn(
                        seedPlayers.stream().map(SeedPlayer::scores365Id).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Player::getSofascoreId, p -> p, (first, dup) -> first));

        // never reset stats of an already seeded player: reruns must not wipe live tournament data
        Map<Long, PlayerWorldCupStats> existingStats = playerWorldCupStatsRepository.findAll().stream()
                .collect(Collectors.toMap(PlayerWorldCupStats::getPlayerId, s -> s));

        Map<Long, Player> resolvedByRapidId = new HashMap<>();
        // frozen pricing anchor: league rating snapshot of players already known to the app
        Map<Long, Double> baseRatingByRapidId = new HashMap<>();
        List<Player> playersToInsert = new ArrayList<>();
        for (SeedPlayer sp : seedPlayers) {
            Player player = byId.get(sp.rapidId());
            if (player == null) player = bySofascoreId.get(sp.scores365Id());
            baseRatingByRapidId.put(sp.rapidId(), snapshotBaseRating(player));
            if (player == null) {
                player = new Player();
                player.setId(sp.rapidId());
                player.setSofascoreId(sp.scores365Id());
                player.setName(Unidecode.decode(sp.name()));
                player.setNationality(sp.teamName());
                if (sp.dateOfBirth() != null) player.setDateOfBirth(LocalDate.parse(sp.dateOfBirth()));
                player.setPosition(sp.position());
                player.setCurrentlyInjured(false);
                player.setPhotoUrl(scoresConfig.getScoresImageBaseUrl()
                        + scoresConfig.getScoresPlayersImageEndPoint() + sp.scores365Id());
                player.setClubPhotoUrl(scoresConfig.getScoresImageBaseUrl()
                        + scoresConfig.getScoresClubsImageEndPoint() + teamScores365Ids.get(sp.teamId()));
                // 'sofascore' players are skipped by the nightly scheduler (removeOutdatedSchedulerDataPlayers)
                player.setUpdatedBy("sofascore");
                playersToInsert.add(player);
            }
            resolvedByRapidId.put(sp.rapidId(), player);
        }

        // saveAll merges: stats must reference the managed copies, not the detached
        // originals, or hibernate sees two Player objects with the same id in the session
        List<Player> insertedPlayers = playerRepository.saveAll(playersToInsert);
        insertedPlayers.forEach(p -> resolvedByRapidId.put(p.getId(), p));

        // two different seed players can resolve to the same existing player row
        // (one matched by rapid id, another by sofascore id when the scheduler stored
        // a different 365 id than our squad match). @MapsId would then give both stats
        // rows the same pk -> one stats row per resolved player only.
        Set<Long> handledPlayerIds = new HashSet<>();
        List<PlayerWorldCupStats> statsToSave = new ArrayList<>();
        for (SeedPlayer sp : seedPlayers) {
            Player player = resolvedByRapidId.get(sp.rapidId());
            if (!handledPlayerIds.add(player.getId())) {
                logger.warn("world cup seed: seed rapidId {} resolved to already-handled player {}, skipping duplicate stats",
                        sp.rapidId(), player.getId());
                continue;
            }
            Double baseRating = baseRatingByRapidId.get(sp.rapidId());
            PlayerWorldCupStats stats = existingStats.get(player.getId());
            if (stats == null) {
                statsToSave.add(new PlayerWorldCupStats(sp.teamId(), baseRating, 0, 0, 0,
                        WorldCupConstants.DEFAULT_RATING, player));
            } else if (stats.getBaseRating() == null) {
                // backfill rows seeded before the baseRating column existed; live stats untouched
                stats.setBaseRating(baseRating);
                statsToSave.add(stats);
            }
        }
        playerWorldCupStatsRepository.saveAll(statsToSave);
        logger.info("world cup seed: inserted {} new players, {} world cup stats rows",
                playersToInsert.size(), statsToSave.size());
    }

    // last season league rating frozen at seed time; 6.5 for players the app has never tracked
    private Double snapshotBaseRating(Player existingPlayer) {
        if (existingPlayer == null || existingPlayer.getLeagueStats() == null
                || existingPlayer.getLeagueStats().getRating() == null) {
            return WorldCupConstants.DEFAULT_RATING;
        }
        return existingPlayer.getLeagueStats().getRating();
    }
}
