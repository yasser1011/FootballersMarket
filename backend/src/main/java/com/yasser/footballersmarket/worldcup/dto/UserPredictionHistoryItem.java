package com.yasser.footballersmarket.worldcup.dto;

import java.time.Instant;

// one settled prediction joined with its fixture result, for a user's prediction-history view.
// awardedPoints is the total points awarded/deducted (already includes bonusPoints); bonusPoints
// is the manual lever component (shown when > 0); exactScorePoints is the exact-score bonus
// actually awarded (0 when the score wasn't nailed), so the client can split the total.
public record UserPredictionHistoryItem(
        Long fixtureId,
        String round,
        Instant date,
        TeamInfo home,
        TeamInfo away,
        Integer homeGoals,
        Integer awayGoals,
        Long winnerTeamId,
        Long predictedWinnerTeamId,
        Integer predictedHomeGoals,
        Integer predictedAwayGoals,
        Integer awardedPoints,
        Integer bonusPoints,
        Integer exactScorePoints
) {
    public record TeamInfo(Long id, String name, String logoUrl) {}
}
