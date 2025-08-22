package com.yasser.footballersmarket.transaction.dto;

public class TransactionRequest {
    private Long playerId;
    private Integer playerSofascoreId;
    private Integer transactionType;
    private Integer price;

    public TransactionRequest(Long playerId, Integer playerSofascoreId, Integer transactionType, Integer price) {
        this.playerId = playerId;
        this.playerSofascoreId = playerSofascoreId;
        this.transactionType = transactionType;
        this.price = price;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public Integer getPlayerSofascoreId() {
        return playerSofascoreId;
    }

    public Integer getPrice() {
        return price;
    }
}
