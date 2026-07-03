package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import com.yasser.footballersmarket.worldcup.dto.UserPredictionHistoryItem;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionRequest;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldCupPredictionServiceTest extends BaseIntegrationTest {

    @Autowired private WorldCupPredictionService predictionService;
    @Autowired private WorldCupPredictionRepository predictionRepository;
    @Autowired private WorldCupFixtureRepository fixtureRepository;
    @Autowired private WorldCupTeamRepository teamRepository;
    @Autowired private UserRepository userRepository;

    private static final long HOME = 16L, AWAY = 1531L;
    private Long userId;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
        fixtureRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(new User("pred", "pass", 3000)).getId();
    }

    private Long upcomingFixture() {
        WorldCupFixture f = new WorldCupFixture(1L, Instant.now().plus(1, ChronoUnit.DAYS),
                "Group Stage - 1", HOME, AWAY, "NS");
        return fixtureRepository.save(f).getId();
    }

    private Long kickedOffFixture() {
        WorldCupFixture f = new WorldCupFixture(2L, Instant.now().minus(1, ChronoUnit.HOURS),
                "Group Stage - 1", HOME, AWAY, "1H");
        return fixtureRepository.save(f).getId();
    }

    @Test
    void createsThenUpdatesPredictionBeforeKickoff() {
        Long fid = upcomingFixture();

        predictionService.upsert(userId, new WorldCupPredictionRequest(fid, HOME, 2, 1));
        WorldCupPrediction first = predictionRepository.findByUserIdAndFixtureId(userId, fid).orElseThrow();
        assertThat(first.getPredictedWinnerTeamId()).isEqualTo(HOME);
        assertThat(first.getPredictedHomeGoals()).isEqualTo(2);

        // upsert again -> same row updated, no duplicate
        predictionService.upsert(userId, new WorldCupPredictionRequest(fid, AWAY, 1, 2));
        assertThat(predictionRepository.findByUserId(userId)).hasSize(1);
        WorldCupPrediction updated = predictionRepository.findByUserIdAndFixtureId(userId, fid).orElseThrow();
        assertThat(updated.getPredictedWinnerTeamId()).isEqualTo(AWAY); // now a draw
    }

    @Test
    void rejectsPredictionAfterKickoff() {
        Long fid = kickedOffFixture();
        assertThatThrownBy(() -> predictionService.upsert(userId, new WorldCupPredictionRequest(fid, HOME, null, null)))
                .isInstanceOf(PredictionClosedException.class);
    }

//    @Test
//    void rejectsScoreInconsistentWithWinner() {
//        Long fid = upcomingFixture();
//        // picked home to win but gave a draw score
//        assertThatThrownBy(() -> predictionService.upsert(userId, new WorldCupPredictionRequest(fid, HOME, 1, 1)))
//                .isInstanceOf(IllegalArgumentException.class);
//    }

    @Test
    void rejectsWinnerNotInFixture() {
        Long fid = upcomingFixture();
        assertThatThrownBy(() -> predictionService.upsert(userId, new WorldCupPredictionRequest(fid, 9999L, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userPredictionHistoryReturnsSettledWithResultAndBadges() {
        teamRepository.save(new WorldCupTeam(HOME, "Mexico", 14, "logoHome"));
        teamRepository.save(new WorldCupTeam(AWAY, "South Africa", 60, "logoAway"));

        // finished fixture: Mexico 2-1 (home advanced)
        WorldCupFixture finished = new WorldCupFixture(2L, Instant.now().minus(2, ChronoUnit.HOURS),
                "Group Stage - 1", HOME, AWAY, "FT");
        finished.setHomeGoals(2);
        finished.setAwayGoals(1);
        finished.setWinnerTeamId(HOME);
        fixtureRepository.save(finished);

        // settled prediction that nailed the exact score, with a manual bonus
        WorldCupPrediction settled = new WorldCupPrediction(userId, 2L, HOME, 2, 1);
        settled.setAwardedPoints(270);
        settled.setBonusPoints(50);
        settled.setSettled(true);
        predictionRepository.save(settled);

        // an unsettled (not-finished) prediction must NOT appear in history
        Long upcoming = upcomingFixture();
        predictionRepository.save(new WorldCupPrediction(userId, upcoming, HOME, null, null));

        List<UserPredictionHistoryItem> history = predictionService.getUserPredictionHistory(userId);

        assertThat(history).hasSize(1);
        UserPredictionHistoryItem item = history.get(0);
        assertThat(item.fixtureId()).isEqualTo(2L);
        assertThat(item.home().name()).isEqualTo("Mexico");
        assertThat(item.away().name()).isEqualTo("South Africa");
        assertThat(item.homeGoals()).isEqualTo(2);
        assertThat(item.awayGoals()).isEqualTo(1);
        assertThat(item.predictedWinnerTeamId()).isEqualTo(HOME);
        assertThat(item.awardedPoints()).isEqualTo(270);
        assertThat(item.bonusPoints()).isEqualTo(50);
        assertThat(item.exactScorePoints()).isEqualTo(90);
    }

    @Test
    void publicFeedHiddenUntilKickoffThenVisible() {
        Long upcoming = upcomingFixture();
        predictionService.upsert(userId, new WorldCupPredictionRequest(upcoming, HOME, null, null));
        // before kickoff: hidden from the public feed
        assertThat(predictionService.getFixturePredictions(upcoming)).isEmpty();
        // owner still sees their own
        assertThat(predictionService.getUserPredictions(userId)).hasSize(1);

        // a fixture already kicked off: predictions are public
        Long live = kickedOffFixture();
        predictionRepository.save(new WorldCupPrediction(userId, live, AWAY, null, null));
        List<WorldCupPredictionResponse> feed = predictionService.getFixturePredictions(live);
        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).username()).isEqualTo("pred");
        assertThat(feed.get(0).predictedWinnerTeamId()).isEqualTo(AWAY);
    }
}
