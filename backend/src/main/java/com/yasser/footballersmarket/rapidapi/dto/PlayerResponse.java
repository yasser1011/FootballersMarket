package com.yasser.footballersmarket.rapidapi.dto;

import java.util.List;

public class PlayerResponse {
    PlayerInfo player;
    List<PlayerStats> statistics;

    public PlayerResponse(){}

    public PlayerResponse(PlayerInfo player, List<PlayerStats> statistics) {
        this.player = player;
        this.statistics = statistics;
    }

    public PlayerInfo getPlayer() {
        return player;
    }

    public void setPlayer(PlayerInfo player) {
        this.player = player;
    }

    public List<PlayerStats> getStatistics() {
        return statistics;
    }

    public void setStatistics(List<PlayerStats> statistics) {
        this.statistics = statistics;
    }
}
