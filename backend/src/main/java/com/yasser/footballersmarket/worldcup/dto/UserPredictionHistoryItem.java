package com.yasser.footballersmarket.worldcup.dto;

import java.time.Instant;

// one settled prediction joined with its fixture result, for a user's prediction-history view.
// awardedPoints is the total points awarded/deducted (already includes bonusPoints); bonusPoints
// is the manual lever component (shown as a badge when > 0); exactScore = the predicted score
// exactly matched the actual result.
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
        boolean exactScore
) {
    public record TeamInfo(Long id, String name, String logoUrl) {}
}
