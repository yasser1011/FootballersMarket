package com.yasser.footballersmarket.worldcup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStatsRepository;
import com.yasser.footballersmarket.scores365.ScoresConfig;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorldCupDataSeederTest extends BaseIntegrationTest {

    @Autowired
    private WorldCupTeamRepository worldCupTeamRepository;
    @Autowired
    private WorldCupFixtureRepository worldCupFixtureRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private PlayerWorldCupStatsRepository playerWorldCupStatsRepository;
    @Autowired
    private com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtRepository playerUserCurrentBoughtRepository;
    @Autowired
    private com.yasser.footballersmarket.transaction.TransactionRepository transactionRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ScoresConfig scoresConfig;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private WorldCupDataSeeder worldCupDataSeeder;

    // mirrors the @Transactional on the seeder's CommandLineRunner entry point;
    // calling seed() on a manually built instance has no spring proxy around it
    private void seedInTransaction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            try {
                worldCupDataSeeder.seed();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // seeding is driven explicitly here (the conditional seeder bean is off in tests,
    // see BaseIntegrationTest) against a wiped db so other test classes can't interfere
    @BeforeAll
    void seedOnce() throws IOException {
        worldCupDataSeeder = new WorldCupDataSeeder(worldCupTeamRepository, worldCupFixtureRepository,
                playerRepository, playerWorldCupStatsRepository, objectMapper, scoresConfig);

        // other test classes may leave rows referencing players (transactions + ownership);
        // both FK player, so clear them before the player delete
        transactionRepository.deleteAll();
        playerUserCurrentBoughtRepository.deleteAll();
        playerWorldCupStatsRepository.deleteAll();
        playerRepository.deleteAll();
        worldCupFixtureRepository.deleteAll();
        worldCupTeamRepository.deleteAll();

        // a player the app already tracks with last season league stats:
        // Thibaut Courtois, rapid id 730 / 365 id 826, present in players.json
        Player existingElite = new Player();
        existingElite.setId(730L);
        existingElite.setSofascoreId(826);
        existingElite.setName("Thibaut Courtois");
        existingElite.setUpdatedBy("sofascore");
        existingElite.setPhotoUrl("https://imagecache.365scores.com/image/upload/v25/Athletes/826");
        existingElite.setLeagueStats(new PlayerLeagueStats(1, 2, 7.5));
        playerRepository.save(existingElite);

        seedInTransaction();
    }

    @Test
    void seedsAllTeamsWithRankAndLogo() {
        List<WorldCupTeam> teams = worldCupTeamRepository.findAll();
        assertThat(teams).hasSize(48);
        assertThat(teams).allSatisfy(t -> {
            assertThat(t.getId()).isNotNull();
            assertThat(t.getName()).isNotBlank();
            // anglicized by unidecode: no accented characters left
            assertThat(t.getName()).matches("\\p{ASCII}+");
            assertThat(t.getFifaRank()).isPositive();
            // logos come from 365scores, not rapid api
            assertThat(t.getLogoUrl()).startsWith("https://imagecache.365scores.com");
        });
    }

    @Test
    void seedsAllGroupStageFixturesReferencingSeededTeams() {
        List<WorldCupFixture> fixtures = worldCupFixtureRepository.findAll();
        assertThat(fixtures).hasSize(72);

        List<Long> teamIds = worldCupTeamRepository.findAll().stream().map(WorldCupTeam::getId).toList();
        assertThat(fixtures).allSatisfy(f -> {
            assertThat(f.getDate()).isAfter(Instant.parse("2026-06-01T00:00:00Z"));
            assertThat(f.getRound()).contains("Group Stage");
            assertThat(teamIds).contains(f.getHomeTeamId(), f.getAwayTeamId());
            assertThat(f.getHomeGoals()).isNull();
            assertThat(f.getAwayGoals()).isNull();
            assertThat(f.getCheckAgainMinutes()).isEqualTo(120);
        });
    }

    @Test
    void seedsAllPlayersIntoPlayerTableProtectedFromScheduler() {
        // db was wiped before seeding: every squad player gets inserted into the player table
        List<Player> players = playerRepository.findAll();
        assertThat(players).hasSize(1241);
        assertThat(players).allSatisfy(p -> {
            assertThat(p.getSofascoreId()).isNotNull();
            assertThat(p.getName()).isNotBlank();
            assertThat(p.getUpdatedBy()).isEqualTo("sofascore");
            assertThat(p.getPhotoUrl()).startsWith("https://imagecache.365scores.com");
        });
    }

    @Test
    void seedsWorldCupStatsRowForEveryPlayerWithDefaults() {
        List<PlayerWorldCupStats> stats = playerWorldCupStatsRepository.findAll();
        assertThat(stats).hasSize(1241);

        List<Long> teamIds = worldCupTeamRepository.findAll().stream().map(WorldCupTeam::getId).toList();
        assertThat(stats).allSatisfy(s -> {
            assertThat(teamIds).contains(s.getTeamId());
            assertThat(s.getGoals()).isZero();
            assertThat(s.getAssists()).isZero();
            assertThat(s.getTotalNumOfGames()).isZero();
            assertThat(s.getRating()).isEqualTo(6.5);
        });
    }

    @Test
    void baseRatingSnapshotsLeagueRatingForKnownPlayersAndDefaultsTheRest() {
        // courtois existed with league stats: his frozen anchor is last season's 7.5 -> elite
        PlayerWorldCupStats courtois = playerWorldCupStatsRepository.findById(730L).orElseThrow();
        assertThat(courtois.getBaseRating()).isEqualTo(7.5);
        // opening price = elite base 600 (wcRating 6.5, 0 games at seed time)
        assertThat(WorldCupPriceCalculator.currentPrice(
                courtois.getBaseRating(), courtois.getRating(), courtois.getTotalNumOfGames()))
                .isEqualTo(600);

        // every player unknown to the app starts at the 6.5 default -> low base
        long defaulted = playerWorldCupStatsRepository.findAll().stream()
                .filter(s -> s.getBaseRating() == 6.5).count();
        assertThat(defaulted).isEqualTo(1240);
    }

    @Test
    void everyTeamHasFullSquad() {
        List<PlayerWorldCupStats> stats = playerWorldCupStatsRepository.findAll();
        var byTeam = stats.stream().collect(Collectors.groupingBy(PlayerWorldCupStats::getTeamId));
        assertThat(byTeam).hasSize(48);
        // 26-man squads, minus the dismissed provider-conflict players
        byTeam.values().forEach(squad -> assertThat(squad).hasSizeBetween(24, 26));
    }

    @Test
    void reseedingDoesNotDuplicateOrResetStats() {
        // simulate live tournament data, then reseed
        PlayerWorldCupStats anyStats = playerWorldCupStatsRepository.findAll().get(0);
        anyStats.setGoals(3);
        playerWorldCupStatsRepository.save(anyStats);

        seedInTransaction();

        assertThat(worldCupTeamRepository.count()).isEqualTo(48);
        assertThat(worldCupFixtureRepository.count()).isEqualTo(72);
        assertThat(playerRepository.count()).isEqualTo(1241);
        assertThat(playerWorldCupStatsRepository.count()).isEqualTo(1241);
        // live stats survive the rerun
        assertThat(playerWorldCupStatsRepository.findById(anyStats.getPlayerId()))
                .hasValueSatisfying(s -> assertThat(s.getGoals()).isEqualTo(3));

        // restore: the db is shared with the other tests in this class
        anyStats.setGoals(0);
        playerWorldCupStatsRepository.save(anyStats);
    }
}
