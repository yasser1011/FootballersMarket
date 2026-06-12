package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionRequest;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorldCupPredictionService {
    private final WorldCupPredictionRepository predictionRepository;
    private final WorldCupFixtureRepository fixtureRepository;
    private final UserRepository userRepository;

    public WorldCupPredictionService(WorldCupPredictionRepository predictionRepository,
                                     WorldCupFixtureRepository fixtureRepository,
                                     UserRepository userRepository) {
        this.predictionRepository = predictionRepository;
        this.fixtureRepository = fixtureRepository;
        this.userRepository = userRepository;
    }

    // create or update the user's prediction for a fixture, allowed only before kickoff
    @Transactional
    public void upsert(Long userId, WorldCupPredictionRequest req) {
        WorldCupFixture fixture = fixtureRepository.findById(req.fixtureId())
                .orElseThrow(() -> new EntityNotFoundException("fixture " + req.fixtureId()));
        if (!Instant.now().isBefore(fixture.getDate())) {
            throw new PredictionClosedException("predictions are closed for this fixture");
        }
        validatePick(req, fixture);

        WorldCupPrediction prediction = predictionRepository
                .findByUserIdAndFixtureId(userId, req.fixtureId())
                .orElseGet(() -> new WorldCupPrediction(userId, req.fixtureId(), null, null, null));
        prediction.setPredictedWinnerTeamId(req.predictedWinnerTeamId());
        prediction.setPredictedHomeGoals(req.predictedHomeGoals());
        prediction.setPredictedAwayGoals(req.predictedAwayGoals());
        predictionRepository.save(prediction);
    }

    private void validatePick(WorldCupPredictionRequest req, WorldCupFixture fixture) {
        Long winner = req.predictedWinnerTeamId();
        if (winner != null && !winner.equals(fixture.getHomeTeamId()) && !winner.equals(fixture.getAwayTeamId())) {
            throw new IllegalArgumentException("predicted winner must be a team in this fixture, or null for a draw");
        }
        Integer home = req.predictedHomeGoals();
        Integer away = req.predictedAwayGoals();
        if ((home == null) != (away == null)) {
            throw new IllegalArgumentException("provide both predicted goals or neither");
        }
        if (home != null) {
            if (home < 0 || away < 0) {
                throw new IllegalArgumentException("predicted goals cannot be negative");
            }
            Long impliedWinner = home > away ? fixture.getHomeTeamId()
                    : home < away ? fixture.getAwayTeamId() : null;
            if (!Objects.equals(impliedWinner, winner)) {
                throw new IllegalArgumentException("predicted score does not match the predicted winner");
            }
        }
    }

    // owner sees all their own predictions, including before kickoff
    @Transactional(readOnly = true)
    public List<WorldCupPredictionResponse> getUserPredictions(Long userId) {
        return toResponses(predictionRepository.findByUserId(userId));
    }

    // public feed: hidden until the fixture kicks off, then everyone's picks + points
    @Transactional(readOnly = true)
    public List<WorldCupPredictionResponse> getFixturePredictions(Long fixtureId) {
        WorldCupFixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new EntityNotFoundException("fixture " + fixtureId));
        if (Instant.now().isBefore(fixture.getDate())) {
            return Collections.emptyList();
        }
        return toResponses(predictionRepository.findByFixtureId(fixtureId));
    }

    private List<WorldCupPredictionResponse> toResponses(List<WorldCupPrediction> predictions) {
        Set<Long> userIds = predictions.stream().map(WorldCupPrediction::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        return predictions.stream()
                .map(p -> new WorldCupPredictionResponse(p.getFixtureId(), p.getUserId(),
                        usernames.get(p.getUserId()), p.getPredictedWinnerTeamId(),
                        p.getPredictedHomeGoals(), p.getPredictedAwayGoals(),
                        p.getAwardedPoints(), p.isSettled()))
                .collect(Collectors.toList());
    }
}
