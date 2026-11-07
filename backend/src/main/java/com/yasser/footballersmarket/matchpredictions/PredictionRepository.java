package com.yasser.footballersmarket.matchpredictions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<MatchPrediction, Long> {

    // upsert lookup: one prediction per user per fixture (enforced by uc_user_fixture)
    Optional<MatchPrediction> findByUserIdAndFixtureId(Long userId, Long fixtureId);

    // settlement: everything still owed points for a fixture that just finished. filtering on
    // settled is what makes a second settlement run a no-op
    List<MatchPrediction> findByFixtureIdAndSettledFalse(Long fixtureId);

    // owner's own predictions, any state
    List<MatchPrediction> findByUserId(Long userId);

    // a user's settled predictions = finished matches only, for the history view
    List<MatchPrediction> findByUserIdAndSettledTrue(Long userId);
}
