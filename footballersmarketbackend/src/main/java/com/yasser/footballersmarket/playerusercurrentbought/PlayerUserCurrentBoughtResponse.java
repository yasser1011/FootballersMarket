package com.yasser.footballersmarket.playerusercurrentbought;

public class PlayerUserCurrentBoughtResponse {
    private Long userId;
    private Long playerId;
    private Boolean playerCurrentlyBoughtByUser;
    private Integer buyPrice;

    public PlayerUserCurrentBoughtResponse(){}

    public PlayerUserCurrentBoughtResponse(Long userId, Long playerId,
                                           Boolean playerCurrentlyBoughtByUser, Integer buyPrice) {
        this.userId = userId;
        this.playerId = playerId;
        this.playerCurrentlyBoughtByUser = playerCurrentlyBoughtByUser;
        this.buyPrice = buyPrice;
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
}
