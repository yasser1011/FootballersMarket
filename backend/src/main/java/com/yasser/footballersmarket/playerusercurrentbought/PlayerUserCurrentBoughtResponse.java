package com.yasser.footballersmarket.playerusercurrentbought;

import java.time.LocalDateTime;

public class PlayerUserCurrentBoughtResponse {
    private Long userId;
    private Long playerId;
    private Boolean playerCurrentlyBoughtByUser;
    private Integer buyPrice;
    // when the player was bought; lets the details page compute the 48h sell-lock countdown.
    // null when the player isn't currently owned.
    private LocalDateTime boughtAt;

    public PlayerUserCurrentBoughtResponse(){}

    public PlayerUserCurrentBoughtResponse(Long userId, Long playerId,
                                           Boolean playerCurrentlyBoughtByUser, Integer buyPrice) {
        this.userId = userId;
        this.playerId = playerId;
        this.playerCurrentlyBoughtByUser = playerCurrentlyBoughtByUser;
        this.buyPrice = buyPrice;
    }

    public PlayerUserCurrentBoughtResponse(Long userId, Long playerId, Boolean playerCurrentlyBoughtByUser,
                                           Integer buyPrice, LocalDateTime boughtAt) {
        this.userId = userId;
        this.playerId = playerId;
        this.playerCurrentlyBoughtByUser = playerCurrentlyBoughtByUser;
        this.buyPrice = buyPrice;
        this.boughtAt = boughtAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Boolean getPlayerCurrentlyBoughtByUser() {
        return playerCurrentlyBoughtByUser;
    }

    public void setPlayerCurrentlyBoughtByUser(Boolean playerCurrentlyBoughtByUser) {
        this.playerCurrentlyBoughtByUser = playerCurrentlyBoughtByUser;
    }

    public Integer getBuyPrice() {
        return buyPrice;
    }

    public LocalDateTime getBoughtAt() {
        return boughtAt;
    }
}
