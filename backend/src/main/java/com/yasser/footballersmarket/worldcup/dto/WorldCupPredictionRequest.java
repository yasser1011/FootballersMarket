package com.yasser.footballersmarket.worldcup.dto;

import javax.validation.constraints.NotNull;

// predictedWinnerTeamId null = draw. goals optional (both or neither); when given they unlock
// the exact-score bonus and must agree with the predicted winner
public record WorldCupPredictionRequest(
        @NotNull Long fixtureId,
        Long predictedWinnerTeamId,
        Integer predictedHomeGoals,
        Integer predictedAwayGoals
) {}
