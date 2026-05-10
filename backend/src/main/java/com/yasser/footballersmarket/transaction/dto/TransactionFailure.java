package com.yasser.footballersmarket.transaction.dto;

public enum TransactionFailure {
    PRICE_MISMATCH("wrong price"),
    MAX_BUY_REACHED("maximum limit reached for buying players"),
    ALREADY_OWNED("player is already bought by this user"),
    NOT_OWNED("player wasn't bought by this user"),
    INSUFFICIENT_POINTS("not enough points to buy the player");

    public final String message;

    TransactionFailure(String message) {
        this.message = message;
    }
}
