package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WorldCupSettlementServiceTest extends BaseIntegrationTest {

    @Autowired private WorldCupSettlementService settlementService;
    @Autowired private WorldCupPredictionRepository predictionRepository;
    @Autowired private WorldCupTeamRepository teamRepository;
    @Autowired private WorldCupFixtureRepository fixtureRepository;
    @Autowired private UserRepository userRepository;

    // Mexico(rank 14, home) beat South Africa(rank 60, away) 2-0; g=46
    private static final long HOME_TEAM = 16L, AWAY_TEAM = 1531L, FIXTURE_ID = 1489369L;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        fixtureRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        teamRepository.save(new WorldCupTeam(HOME_TEAM, "Mexico", 14, "logo"));
        teamRepository.save(new WorldCupTeam(AWAY_TEAM, "South Africa", 60, "logo"));

        WorldCupFixture fixture = new WorldCupFixture(FIXTURE_ID, Instant.now(), "Group Stage - 1",
                HOME_TEAM, AWAY_TEAM, "FT");
        fixture.setHomeGoals(2);
        fixture.setAwayGoals(0);
        fixture.setWinnerTeamId(HOME_TEAM); // Mexico won
        fixtureRepository.save(fixture);
    }

    private Long newUser(String name) {
        return userRepository.save(new User(name, "pass", 3000)).getId();
    }

    private WorldCupFixture fixture() {
        return fixtureRepository.findById(FIXTURE_ID).orElseThrow();
    }

    @Test
    void correctFavoritePickCreditsPoints() {
        Long userId = newUser("fav");
        predictionRepository.save(new WorldCupPrediction(userId, FIXTURE_ID, HOME_TEAM, null, null));

        settlementService.settleFixture(fixture());

        // favorite win at g=46 -> +65
        assertThat(userRepository.findById(userId).orElseThrow().getPoints()).isEqualTo(3000 + 65);
        WorldCupPrediction settled = predictionRepository.findByUserIdAndFixtureId(userId, FIXTURE_ID).orElseThrow();
        assertThat(settled.isSettled()).isTrue();
        assertThat(settled.getAwardedPoints()).isEqualTo(65);
    }

    @Test
    void exactScoreAddsBonus() {
        Long userId = newUser("exact");
        predictionRepository.save(new WorldCupPrediction(userId, FIXTURE_ID, HOME_TEAM, 2, 0));

        settlementService.settleFixture(fixture());

        // 65 + 150 exact
        assertThat(userRepository.findById(userId).orElseThrow().getPoints()).isEqualTo(3000 + 65 + 150);
    }

    @Test
    void wrongUnderdogPickDeductsPoints() {
        Long userId = newUser("dog");
        predictionRepository.save(new WorldCupPrediction(userId, FIXTURE_ID, AWAY_TEAM, null, null));

        settlementService.settleFixture(fixture());

        // predicted underdog (SA), Mexico won -> underdog miss at g=46 = -65
        assertThat(userRepository.findById(userId).orElseThrow().getPoints()).isEqualTo(3000 - 65);
    }

    @Test
    void manualBonusIsAddedOnTop() {
        Long userId = newUser("bonus");
        WorldCupPrediction p = new WorldCupPrediction(userId, FIXTURE_ID, HOME_TEAM, null, null);
        p.setBonusPoints(500);
        predictionRepository.save(p);

        settlementService.settleFixture(fixture());

        assertThat(userRepository.findById(userId).orElseThrow().getPoints()).isEqualTo(3000 + 65 + 500);
    }

    @Test
    void penaltyNeverPushesWalletNegative() {
        Long userId = newUser("broke");
        User u = userRepository.findById(userId).orElseThrow();
        u.setPoints(40);
        userRepository.save(u);
        // predicted draw, Mexico won -> draw miss at g=46 is -48, but only 40 points -> floored at 0
        predictionRepository.save(new WorldCupPrediction(userId, FIXTURE_ID, null, null, null));

        settlementService.settleFixture(fixture());

        assertThat(userRepository.findById(userId).orElseThrow().getPoints()).isZero();
    }

    @Test
    void reSettlingDoesNotDoublePay() {
        Long userId = newUser("idem");
        predictionRepository.save(new WorldCupPrediction(userId, FIXTURE_ID, HOME_TEAM, null, null));

        settlementService.settleFixture(fixture());
        settlementService.settleFixture(fixture()); // rerun

        assertThat(userRepository.findById(userId).orElseThrow().getPoints()).isEqualTo(3000 + 65);
    }
}
