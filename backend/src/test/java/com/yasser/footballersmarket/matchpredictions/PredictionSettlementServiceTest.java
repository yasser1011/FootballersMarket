package com.yasser.footballersmarket.matchpredictions;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionSettlementServiceTest {

    private static final Long FIXTURE_ID = 1557427L;
    private static final Long HOME_TEAM_ID = 42L;   // arsenal
    private static final Long AWAY_TEAM_ID = 50L;   // man city
    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;

    // tier 3 with a zero gap keeps the pick multipliers at exactly 1.0, so the expected points are
    // the calculator's bases: +110 correct, -85 wrong, +90 more for an exact score.
    // the tier and gap themselves come from a mocked resolver, so these tests exercise settlement
    // rather than the tiering rules
    private static final FixtureTierResolver.FixtureWeighting TIER_3_NO_GAP =
            new FixtureTierResolver.FixtureWeighting(3, 0.0, false);
    private static final int CORRECT_POINTS = 110;
    private static final int WRONG_POINTS = -85;
    private static final int EXACT_SCORE_BONUS = 90;

    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private FixtureTierResolver fixtureTierResolver;
    @Mock
    private UserRepository userRepository;

    private PredictionSettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new PredictionSettlementService(
                predictionRepository, fixtureTierResolver, userRepository);
    }

    @Test
    void shouldCreditUserAndMarkPredictionSettledOnACorrectPick() {
        MatchPrediction prediction = prediction(USER_ID, HOME_TEAM_ID, null, null);
        User user = user(USER_ID, 1000);
        givenHomeWinWith(prediction);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        assertThat(user.getPoints()).isEqualTo(1000 + CORRECT_POINTS);
        assertThat(prediction.getAwardedPoints()).isEqualTo(CORRECT_POINTS);
        assertThat(prediction.isSettled()).isTrue();
        verify(userRepository).save(user);
        verify(predictionRepository).saveAll(List.of(prediction));
    }

    @Test
    void shouldDeductPointsOnAWrongPick() {
        MatchPrediction prediction = prediction(USER_ID, AWAY_TEAM_ID, null, null);
        User user = user(USER_ID, 1000);
        givenHomeWinWith(prediction);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        assertThat(user.getPoints()).isEqualTo(1000 + WRONG_POINTS);
        assertThat(prediction.getAwardedPoints()).isEqualTo(WRONG_POINTS);
        assertThat(prediction.isSettled()).isTrue();
    }

    @Test
    void shouldFloorTheWalletAtZeroButStillRecordTheFullDeduction() {
        MatchPrediction prediction = prediction(USER_ID, AWAY_TEAM_ID, null, null);
        User user = user(USER_ID, 50);   // smaller than the 85 point penalty
        givenHomeWinWith(prediction);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        // the balance cannot go negative, but what the pick was worth is recorded as-is
        assertThat(user.getPoints()).isZero();
        assertThat(prediction.getAwardedPoints()).isEqualTo(WRONG_POINTS);
    }

    @Test
    void shouldAddTheExactScoreBonusOnTopOfACorrectPick() {
        // fixture finishes 2-1 and the prediction called 2-1
        MatchPrediction prediction = prediction(USER_ID, HOME_TEAM_ID, 2, 1);
        User user = user(USER_ID, 1000);
        givenHomeWinWith(prediction);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        assertThat(prediction.getAwardedPoints()).isEqualTo(CORRECT_POINTS + EXACT_SCORE_BONUS);
        assertThat(user.getPoints()).isEqualTo(1000 + CORRECT_POINTS + EXACT_SCORE_BONUS);
    }

    @Test
    void shouldNotAddTheExactScoreBonusWhenTheScorelineMissed() {
        // right winner, wrong scoreline: 3-0 predicted against a 2-1 result
        MatchPrediction prediction = prediction(USER_ID, HOME_TEAM_ID, 3, 0);
        User user = user(USER_ID, 1000);
        givenHomeWinWith(prediction);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        assertThat(prediction.getAwardedPoints()).isEqualTo(CORRECT_POINTS);
    }

    @Test
    void shouldSettleNothingWhenEveryPredictionIsAlreadySettled() {
        when(predictionRepository.findByFixtureIdAndSettledFalse(FIXTURE_ID)).thenReturn(List.of());

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        // the settled latch: a rerun must not touch a wallet or re-resolve the fixture
        verify(userRepository, never()).findByIdForUpdate(any());
        verify(userRepository, never()).save(any());
        verify(predictionRepository, never()).saveAll(any());
    }

    @Test
    void shouldStillSettleAPredictionWhoseUserHasGone() {
        MatchPrediction prediction = prediction(USER_ID, HOME_TEAM_ID, null, null);
        givenHomeWinWith(prediction);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        // skipping the credit but leaving the row unsettled would re-poll the fixture forever
        assertThat(prediction.isSettled()).isTrue();
        assertThat(prediction.getAwardedPoints()).isEqualTo(CORRECT_POINTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldSettleEveryPredictionOnTheFixture() {
        MatchPrediction correct = prediction(USER_ID, HOME_TEAM_ID, null, null);
        MatchPrediction wrong = prediction(OTHER_USER_ID, AWAY_TEAM_ID, null, null);
        User winner = user(USER_ID, 1000);
        User loser = user(OTHER_USER_ID, 1000);
        givenHomeWinWith(correct, wrong);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(winner));
        when(userRepository.findByIdForUpdate(OTHER_USER_ID)).thenReturn(Optional.of(loser));

        settlementService.onFixtureFinished(homeWinFixture(), anyResult());

        assertThat(winner.getPoints()).isEqualTo(1000 + CORRECT_POINTS);
        assertThat(loser.getPoints()).isEqualTo(1000 + WRONG_POINTS);
        assertThat(correct.isSettled()).isTrue();
        assertThat(wrong.isSettled()).isTrue();
        verify(predictionRepository).saveAll(List.of(correct, wrong));
    }

    // the home side won 2-1, so a HOME_TEAM_ID pick is correct and anything else is wrong
    private Fixture homeWinFixture() {
        return new Fixture(FIXTURE_ID, Instant.now().minusSeconds(7200), 39L,
                "Regular Season - 12", "FT", HOME_TEAM_ID, AWAY_TEAM_ID, 2, 1, HOME_TEAM_ID);
    }

    // settlement scores from the fixture row alone and ignores the per-player payload, so any result will do
    private FixtureResponseDto.Response anyResult() {
        return new FixtureResponseDto.Response(null, null, null, null, null);
    }

    private void givenHomeWinWith(MatchPrediction... predictions) {
        when(predictionRepository.findByFixtureIdAndSettledFalse(FIXTURE_ID))
                .thenReturn(List.of(predictions));
        when(fixtureTierResolver.resolve(any())).thenReturn(TIER_3_NO_GAP);
    }

    private MatchPrediction prediction(Long userId, Long winnerTeamId, Integer homeScore, Integer awayScore) {
        return new MatchPrediction(FIXTURE_ID, userId, homeScore, awayScore, winnerTeamId);
    }

    private User user(Long id, int points) {
        return new User(id, "user" + id, "pass", points);
    }
}
