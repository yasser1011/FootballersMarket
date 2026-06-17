package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserRepository;
import com.yasser.footballersmarket.worldcup.dto.UserPredictionHistoryItem;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionRequest;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WorldCupPredictionService {
    private final WorldCupPredictionRepository predictionRepository;
    private final WorldCupFixtureRepository fixtureRepository;
    private final WorldCupTeamRepository teamRepository;
    private final UserRepository userRepository;

    public WorldCupPredictionService(WorldCupPredictionRepository predictionRepository,
                                     WorldCupFixtureRepository fixtureRepository,
                                     WorldCupTeamRepository teamRepository,
                                     UserRepository userRepository) {
        this.predictionRepository = predictionRepository;
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
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

    // a user's finished-game prediction history (settled only), each joined with its fixture result
    // and teams, newest first. public — finished predictions are already public.
    @Transactional(readOnly = true)
    public List<UserPredictionHistoryItem> getUserPredictionHistory(Long userId) {
        List<WorldCupPrediction> predictions = predictionRepository.findByUserIdAndSettledTrue(userId);
        if (predictions.isEmpty()) return Collections.emptyList();

        Set<Long> fixtureIds = predictions.stream()
                .map(WorldCupPrediction::getFixtureId).collect(Collectors.toSet());
        Map<Long, WorldCupFixture> fixtures = fixtureRepository.findAllById(fixtureIds).stream()
                .collect(Collectors.toMap(WorldCupFixture::getId, f -> f));

        Set<Long> teamIds = fixtures.values().stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WorldCupTeam> teams = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(WorldCupTeam::getId, t -> t));

        return predictions.stream()
                .map(p -> toHistoryItem(p, fixtures.get(p.getFixtureId()), teams))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(UserPredictionHistoryItem::date,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    private UserPredictionHistoryItem toHistoryItem(WorldCupPrediction p, WorldCupFixture f,
                                                    Map<Long, WorldCupTeam> teams) {
        if (f == null) return null; // fixture row gone; skip
        boolean exactScore = isExactScore(p, f);
        return new UserPredictionHistoryItem(
                f.getId(), f.getRound(), f.getDate(),
                teamInfo(teams.get(f.getHomeTeamId())), teamInfo(teams.get(f.getAwayTeamId())),
                f.getHomeGoals(), f.getAwayGoals(), f.getWinnerTeamId(),
                p.getPredictedWinnerTeamId(), p.getPredictedHomeGoals(), p.getPredictedAwayGoals(),
                p.getAwardedPoints(), p.getBonusPoints(), exactScore);
    }

    private UserPredictionHistoryItem.TeamInfo teamInfo(WorldCupTeam team) {
        return team == null ? null
                : new UserPredictionHistoryItem.TeamInfo(team.getId(), team.getName(), team.getLogoUrl());
    }

    private List<WorldCupPredictionResponse> toResponses(List<WorldCupPrediction> predictions) {
        Set<Long> userIds = predictions.stream().map(WorldCupPrediction::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        // fixtures are needed only to flag exact-score predictions (compare predicted vs actual goals)
        Set<Long> fixtureIds = predictions.stream()
                .map(WorldCupPrediction::getFixtureId).collect(Collectors.toSet());
        Map<Long, WorldCupFixture> fixtures = fixtureRepository.findAllById(fixtureIds).stream()
                .collect(Collectors.toMap(WorldCupFixture::getId, f -> f));

        return predictions.stream()
                .map(p -> new WorldCupPredictionResponse(p.getFixtureId(), p.getUserId(),
                        usernames.get(p.getUserId()), p.getPredictedWinnerTeamId(),
                        p.getPredictedHomeGoals(), p.getPredictedAwayGoals(),
                        p.getAwardedPoints(), p.getBonusPoints(),
                        isExactScore(p, fixtures.get(p.getFixtureId())), p.isSettled()))
                .collect(Collectors.toList());
    }

    private boolean isExactScore(WorldCupPrediction p, WorldCupFixture f) {
        return f != null && p.getPredictedHomeGoals() != null && p.getPredictedAwayGoals() != null
                && Objects.equals(p.getPredictedHomeGoals(), f.getHomeGoals())
                && Objects.equals(p.getPredictedAwayGoals(), f.getAwayGoals());
    }
}
