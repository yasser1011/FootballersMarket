package com.yasser.footballersmarket.worldcup.dto;

import java.time.Instant;

// a world cup fixture for the public fixtures view: kickoff, round, both teams (resolved from
// world_cup_team for name/logo/rank) and the result so far (goals/winner null until played)
public record WorldCupFixtureResponse(
        Long id,
        Instant date,
        String round,
        String status,
        TeamInfo home,
        TeamInfo away,
        Integer homeGoals,
        Integer awayGoals,
        Long winnerTeamId
) {
    public record TeamInfo(Long id, String name, String logoUrl, Integer fifaRank) {}
}
