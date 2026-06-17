package com.yasser.footballersmarket.worldcup.dto;

// awardedPoints is the total (formula + bonus), null until the fixture is settled.
// bonusPoints is the manual lever component; exactScore = the predicted score exactly matched
// the result. both let the client break the total into result / exact-score / bonus parts.
public record WorldCupPredictionResponse(
        Long fixtureId,
        Long userId,
        String username,
        Long predictedWinnerTeamId,
        Integer predictedHomeGoals,
        Integer predictedAwayGoals,
        Integer awardedPoints,
        Integer bonusPoints,
        boolean exactScore,
        boolean settled
) {}
