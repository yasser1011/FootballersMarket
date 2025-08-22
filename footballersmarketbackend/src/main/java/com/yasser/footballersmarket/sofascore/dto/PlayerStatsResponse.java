package com.yasser.footballersmarket.sofascore.dto;

public class PlayerStatsResponse {
    private PlayerLeagueStatsDetails statistics;

    public PlayerStatsResponse(){}

    public PlayerStatsResponse(PlayerLeagueStatsDetails statistics) {
        this.statistics = statistics;
    }

    public PlayerLeagueStatsDetails getStatistics() {
        return statistics;
    }
}
