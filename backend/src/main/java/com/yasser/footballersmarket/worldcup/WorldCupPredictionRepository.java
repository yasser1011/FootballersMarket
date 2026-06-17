package com.yasser.footballersmarket.worldcup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorldCupPredictionRepository extends JpaRepository<WorldCupPrediction, Long> {
    // upsert lookup: one prediction per user per fixture (enforced by the unique constraint)
    Optional<WorldCupPrediction> findByUserIdAndFixtureId(Long userId, Long fixtureId);

    // settlement: all unsettled predictions for a fixture that just finished
    List<WorldCupPrediction> findByFixtureIdAndSettledFalse(Long fixtureId);

    // owner's own predictions (all states)
    List<WorldCupPrediction> findByUserId(Long userId);

    // a user's settled predictions = finished games only; for the public prediction-history view
    List<WorldCupPrediction> findByUserIdAndSettledTrue(Long userId);

    // public feed: every prediction for a fixture (only exposed after kickoff)
    List<WorldCupPrediction> findByFixtureId(Long fixtureId);
}
