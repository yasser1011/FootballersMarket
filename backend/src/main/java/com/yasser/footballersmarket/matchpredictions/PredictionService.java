package com.yasser.footballersmarket.matchpredictions;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.matchpredictions.dto.PredictionReqDto;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Objects;

@Service
public class PredictionService {

    // a scoreline this high is a data-entry mistake, not a prediction
    private static final int MAX_PREDICTED_GOALS = 20;

    private final PredictionRepository predictionRepository;
    private final FixtureRepository fixtureRepository;

    public PredictionService(PredictionRepository predictionRepository, FixtureRepository fixtureRepository){
        this.predictionRepository = predictionRepository;
        this.fixtureRepository = fixtureRepository;
    }

    // create or update the user's prediction for a fixture, allowed only before kickoff.
    // not transactional: the save is the only write and nothing follows it, so it is already atomic
    // on its own. one prediction per user per fixture is enforced by uc_user_fixture, not by a
    // transaction -- there is no row to lock before the first insert
    public void savePrediction(Long userId, PredictionReqDto predictionReq){
        // only discovered fixtures are predictable, so this doubles as a check that the fixture is
        // one we actually track
        Fixture fixture = fixtureRepository.findById(predictionReq.fixtureId())
                .orElseThrow(() -> new EntityNotFoundException("fixture " + predictionReq.fixtureId()));

        // every league fixture is stored for player stats, but only the important ones are predictable
        if (!fixture.isImportant()) {
            throw new IllegalArgumentException("predictions are not open for this fixture");
        }

        if (!Instant.now().isBefore(fixture.getDate())) {
            throw new PredictionClosedException("predictions are closed for this fixture");
        }
        validatePick(predictionReq, fixture);

        MatchPrediction matchPrediction = predictionRepository
                .findByUserIdAndFixtureId(userId, predictionReq.fixtureId())
                .orElseGet(() -> new MatchPrediction(predictionReq.fixtureId(), userId, null, null, null));

        matchPrediction.setHomeScore(predictionReq.homeScore());
        matchPrediction.setAwayScore(predictionReq.awayScore());
        matchPrediction.setWinnerTeamId(predictionReq.winnerTeamId());
        predictionRepository.save(matchPrediction);
    }

    private void validatePick(PredictionReqDto req, Fixture fixture) {
        Long winner = req.winnerTeamId();

        // an all-null request would otherwise pass every check below and settle as a predicted draw,
        // because a null winner means "draw" -- so an empty submission could win points
        if (winner == null && req.homeScore() == null && req.awayScore() == null) {
            throw new IllegalArgumentException("provide a predicted winner or a predicted score");
        }

        if (winner != null
                && !winner.equals(fixture.getHomeTeamId())
                && !winner.equals(fixture.getAwayTeamId())) {
            throw new IllegalArgumentException("predicted winner must be a team in this fixture, or null for a draw");
        }

        Integer home = req.homeScore();
        Integer away = req.awayScore();
        if ((home == null) != (away == null)) {
            throw new IllegalArgumentException("provide both predicted scores or neither");
        }
        if (home == null) return;

        if (home < 0 || away < 0) {
            throw new IllegalArgumentException("predicted scores cannot be negative");
        }
        if (home > MAX_PREDICTED_GOALS || away > MAX_PREDICTED_GOALS) {
            throw new IllegalArgumentException("predicted scores cannot exceed " + MAX_PREDICTED_GOALS);
        }

        // the scoreline and the named winner have to tell the same story. a level score with no
        // winner is a predicted draw, which is valid for a league fixture
        Long impliedWinner = home > away ? fixture.getHomeTeamId()
                : home < away ? fixture.getAwayTeamId() : null;
        if (!Objects.equals(impliedWinner, winner)) {
            throw new IllegalArgumentException("predicted score does not match the predicted winner");
        }
    }
}
