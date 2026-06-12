package com.yasser.footballersmarket.worldcup.dto;

// awardedPoints is null until the fixture is settled
public record WorldCupPredictionResponse(
        Long fixtureId,
        Long userId,
        String username,
        Long predictedWinnerTeamId,
        Integer predictedHomeGoals,
        Integer predictedAwayGoals,
        Integer awardedPoints,
        boolean settled
) {}
