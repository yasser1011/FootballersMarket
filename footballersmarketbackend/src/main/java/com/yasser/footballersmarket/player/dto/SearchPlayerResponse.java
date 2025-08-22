package com.yasser.footballersmarket.player.dto;

import com.yasser.footballersmarket.player.Player;

public class SearchPlayerResponse {
    private Integer status;
    private Player player;

    public SearchPlayerResponse(){}

    public SearchPlayerResponse(Integer status, Player player) {
        this.status = status;
        this.player = player;
    }

    public Integer getStatus() {
        return status;
    }

    public Player getPlayer() {
        return player;
    }
}
