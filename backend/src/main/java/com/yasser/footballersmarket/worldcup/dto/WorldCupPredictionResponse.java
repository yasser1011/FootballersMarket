package com.yasser.footballersmarket.worldcup.dto;

// awardedPoints is the total (formula + bonus), null until the fixture is settled.
// bonusPoints is the manual lever component; exactScorePoints is the exact-score bonus actually
// awarded (0 when the score wasn't nailed). together they let the client break the total into
// result / exact-score / bonus parts without knowing the reward constants.
public record WorldCupPredictionResponse(
        Long fixtureId,
        Long userId,
        String username,
        Long predictedWinnerTeamId,
        Integer predictedHomeGoals,
        Integer predictedAwayGoals,
        Integer awardedPoints,
        Integer bonusPoints,
        Integer exactScorePoints,
        boolean settled
) {}
