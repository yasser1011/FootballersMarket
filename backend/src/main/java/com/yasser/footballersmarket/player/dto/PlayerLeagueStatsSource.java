package com.yasser.footballersmarket.player.dto;

public class PlayerLeagueStatsSource {
    private String playerStatsSource;
    private Integer playerId;

    public PlayerLeagueStatsSource(){}

    public PlayerLeagueStatsSource(String playerStatsSource, Integer playerId) {
        this.playerStatsSource = playerStatsSource;
        this.playerId = playerId;
    }

    public String getPlayerStatsSource() {
        return playerStatsSource;
    }

    public Integer getPlayerId() {
        return playerId;
    }
}
