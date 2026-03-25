package com.yasser.footballersmarket.sofascore.dto;

import com.yasser.footballersmarket.sofascore.dto.PlayerDetails;

public class PlayerDetailsResponse {
    private PlayerDetails player;

    public PlayerDetailsResponse(){}

    public PlayerDetailsResponse(PlayerDetails player) {
        this.player = player;
    }

    public PlayerDetails getPlayer() {
        return player;
    }
}
