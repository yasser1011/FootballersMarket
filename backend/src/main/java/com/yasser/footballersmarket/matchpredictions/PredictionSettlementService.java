package com.yasser.footballersmarket.matchpredictions;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureFinishedHandler;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.matchpredictions.PredictionRewardCalculator.MatchOutcome;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// awards or deducts points for every prediction on a finished fixture and marks them settled.
@Service
public class PredictionSettlementService implements FixtureFinishedHandler {

    private final PredictionRepository predictionRepository;
    private final FixtureTierResolver fixtureTierResolver;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(PredictionSettlementService.class);

    public PredictionSettlementService(PredictionRepository predictionRepository,
                                       FixtureTierResolver fixtureTierResolver,
                                       UserRepository userRepository) {
        this.predictionRepository = predictionRepository;
        this.fixtureTierResolver = fixtureTierResolver;
        this.userRepository = userRepository;
    }

    // the annotation sits HERE rather than on settleFixture: this is the method the caller invokes
    // through the proxy, and a self-call to settleFixture would bypass the proxy and ignore it.
    //
    // plain @Transactional: applyResult has no transaction of its own, so this always starts a fresh
    // one, and a settlement failure rolls back only the payout -- the fixture's result still stands.
    // if a transaction is ever added to the caller, this must become REQUIRES_NEW: joining it would
    // mark the shared transaction rollback-only, the caller's catch would not clear that, and the
    // fixture's own commit would fail with UnexpectedRollbackException (see FixtureFinishedHandler)
    @Override
    @Transactional
    public void onFixtureFinished(Fixture fixture, FixtureResponseDto.Response result) {
        // settlement scores from the fixture row alone; the per-player payload is not its concern
        settleFixture(fixture);
    }

    // idempotent: only unsettled predictions are read, so a rerun is a no-op -- which is what makes
    // a failed settlement safe to simply log and retry later.
    //
    // private and unannotated: a self-call from onFixtureFinished would bypass the proxy and ignore
    // any @Transactional here anyway. the transaction this runs in is the one onFixtureFinished
    // opens -- which is what covers the whole fixture's payout (every prediction both credits a
    // wallet and is marked settled, and those must not come apart) and what holds the pessimistic
    // lock taken in creditUser, since a row lock lives only as long as its transaction
    private void settleFixture(Fixture fixture) {
        List<MatchPrediction> predictions =
                predictionRepository.findByFixtureIdAndSettledFalse(fixture.getId());
        if (predictions.isEmpty()) return;

        FixtureTierResolver.FixtureWeighting weighting = fixtureTierResolver.resolve(fixture);
        MatchOutcome actual = outcomeOf(fixture.getWinnerTeamId(), fixture);

        for (MatchPrediction prediction : predictions) {
            MatchOutcome predicted = outcomeOf(prediction.getWinnerTeamId(), fixture);

            int delta = PredictionRewardCalculator.calculatePoints(
                    weighting.tier(), weighting.gapNorm(),
                    fixture.getHomeTeamPosition(), fixture.getAwayTeamPosition(),
                    predicted, actual, isExactScore(prediction, fixture),
                    weighting.championsLeague());

            // does a save here so need transactional
            creditUser(prediction, delta);

            // the nominal value of the prediction, recorded as-is: the wallet may have floored at
            // zero, but what the pick was worth does not change
            prediction.setAwardedPoints(delta);
            prediction.setSettled(true);
        }

        predictionRepository.saveAll(predictions);
        logger.info("settlement: fixture {} settled {} prediction(s)", fixture.getId(), predictions.size());
    }

    // the wallet row is locked for the read-modify-write: settlement runs on the scheduler thread
    // while the same user may be buying or selling on a request thread, and both write usr.points.
    // the zero floor keeps a deduction from pushing a balance negative.
    //
    // a prediction whose user no longer exists is still settled, just not credited -- otherwise one
    // orphan row would throw, roll back the fixture, and leave it re-polling forever
    private void creditUser(MatchPrediction prediction, int delta) {
        User user = userRepository.findByIdForUpdate(prediction.getUserId()).orElse(null);
        if (user == null) {
            logger.warn("settlement: skipping prediction {} for missing user {}",
                    prediction.getId(), prediction.getUserId());
            return;
        }
        user.setPoints(Math.max(0, user.getPoints() + delta));
        userRepository.save(user);
    }

    // null winner = draw, otherwise whichever side the api flagged
    private MatchOutcome outcomeOf(Long winnerTeamId, Fixture fixture) {
        if (winnerTeamId == null) return MatchOutcome.DRAW;
        return winnerTeamId.equals(fixture.getHomeTeamId())
                ? MatchOutcome.HOME_WIN
                : MatchOutcome.AWAY_WIN;
    }

    // only when the user gave a scoreline and it matches the final goals
    private boolean isExactScore(MatchPrediction prediction, Fixture fixture) {
        return prediction.getHomeScore() != null && prediction.getAwayScore() != null
                && Objects.equals(prediction.getHomeScore(), fixture.getHomeGoals())
                && Objects.equals(prediction.getAwayScore(), fixture.getAwayGoals());
    }
}
