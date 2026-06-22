package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

import static com.yasser.footballersmarket.worldcup.PredictionRewardCalculator.MatchOutcome;
import static com.yasser.footballersmarket.worldcup.PredictionRewardCalculator.MatchOutcome.AWAY_WIN;
import static com.yasser.footballersmarket.worldcup.PredictionRewardCalculator.MatchOutcome.DRAW;
import static com.yasser.footballersmarket.worldcup.PredictionRewardCalculator.MatchOutcome.HOME_WIN;

// settles all predictions for a finished fixture: awards/deducts points and marks them settled.
// called by the results scheduler once a fixture's result (winnerTeamId + goals) is known.
@Service
public class WorldCupSettlementService {
    private final WorldCupPredictionRepository predictionRepository;
    private final WorldCupTeamRepository worldCupTeamRepository;
    private final UserRepository userRepository;

    private final Logger logger = LoggerFactory.getLogger(WorldCupSettlementService.class);

    public WorldCupSettlementService(WorldCupPredictionRepository predictionRepository,
                                     WorldCupTeamRepository worldCupTeamRepository,
                                     UserRepository userRepository) {
        this.predictionRepository = predictionRepository;
        this.worldCupTeamRepository = worldCupTeamRepository;
        this.userRepository = userRepository;
    }

    // idempotent: only unsettled predictions are processed, so a second call (poller rerun) is a no-op.
    //
    // each user's points are updated under a PESSIMISTIC ROW LOCK (findByIdForUpdate) so a
    // concurrent buy/sell on the same wallet can't clobber this award (lost update).
    //
    // STUDY LATER — atomic increment alternative: instead of lock-read-modify-write, a single
    // statement "UPDATE usr SET points = GREATEST(0, points + :delta) WHERE id = :id" updates the
    // balance atomically (the DB computes points + delta under its own row lock) and applies the
    // zero-floor in the same step, with no explicit lock code. It serializes correctly against the
    // existing buy/sell FOR UPDATE lock. Revisit and benchmark once comfortable with it; the
    // pessimistic-lock version below is correct as written.
    @Transactional
    public void settleFixture(WorldCupFixture fixture) {
        List<WorldCupPrediction> predictions =
                predictionRepository.findByFixtureIdAndSettledFalse(fixture.getId());
        if (predictions.isEmpty()) return;

        int homeRank = worldCupTeamRepository.findById(fixture.getHomeTeamId())
                .map(WorldCupTeam::getFifaRank)
                .orElseThrow(() -> new EntityNotFoundException("wc team " + fixture.getHomeTeamId()));
        int awayRank = worldCupTeamRepository.findById(fixture.getAwayTeamId())
                .map(WorldCupTeam::getFifaRank)
                .orElseThrow(() -> new EntityNotFoundException("wc team " + fixture.getAwayTeamId()));
        MatchOutcome actual = outcomeFor(fixture.getWinnerTeamId(), fixture);

        for (WorldCupPrediction prediction : predictions) {
            MatchOutcome predicted = outcomeFor(prediction.getPredictedWinnerTeamId(), fixture);
            boolean exactScore = isExactScore(prediction, fixture);
            int formulaPoints = PredictionRewardCalculator.calculatePoints(
                    homeRank, awayRank, predicted, actual, exactScore);
            int bonus = prediction.getBonusPoints() == null ? 0 : prediction.getBonusPoints();
            int delta = formulaPoints + bonus;

            // lock the wallet row, apply with zero-floor so a penalty can't push the user negative.
            // a deleted user (e.g. removed directly in the db) leaves orphan predictions with no
            // wallet to credit -> skip the credit but still settle the row, so one orphan can't
            // throw and roll back the whole fixture (which would re-poll and loop forever).
            User user = userRepository.findByIdForUpdate(prediction.getUserId()).orElse(null);
            if (user != null) {
                user.setPoints(Math.max(0, user.getPoints() + delta));
                userRepository.save(user);
            } else {
                logger.warn("world cup settlement: skipping prediction {} for missing user {}",
                        prediction.getId(), prediction.getUserId());
            }

            // awardedPoints is the nominal result of the prediction (formula + bonus), shown to users;
            // the wallet may have floored at 0, but the prediction's value is recorded as-is
            prediction.setAwardedPoints(delta);
            prediction.setSettled(true);
        }
        predictionRepository.saveAll(predictions);
        logger.info("world cup settlement: fixture {} settled {} predictions", fixture.getId(), predictions.size());
    }

    // null winner = draw; otherwise whichever side advanced
    private MatchOutcome outcomeFor(Long winnerTeamId, WorldCupFixture fixture) {
        if (winnerTeamId == null) return DRAW;
        if (winnerTeamId.equals(fixture.getHomeTeamId())) return HOME_WIN;
        return AWAY_WIN;
    }

    // exact bonus only when the user gave a score and it equals the final goals
    private boolean isExactScore(WorldCupPrediction prediction, WorldCupFixture fixture) {
        return prediction.getPredictedHomeGoals() != null && prediction.getPredictedAwayGoals() != null
                && prediction.getPredictedHomeGoals().equals(fixture.getHomeGoals())
                && prediction.getPredictedAwayGoals().equals(fixture.getAwayGoals());
    }
}
