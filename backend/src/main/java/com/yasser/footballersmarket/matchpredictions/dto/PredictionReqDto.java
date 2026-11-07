package com.yasser.footballersmarket.matchpredictions.dto;

public record PredictionReqDto(
        Long fixtureId, Integer homeScore, Integer awayScore, Long winnerTeamId
) {
}
