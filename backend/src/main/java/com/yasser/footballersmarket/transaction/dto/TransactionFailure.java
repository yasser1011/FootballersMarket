package com.yasser.footballersmarket.transaction.dto;

public enum TransactionFailure {
    // type is the stable identifier the frontend branches on (carried out on TransactionError.errorType);
    // "price" is kept for backwards compatibility, the rest expose the specific failure.
    PRICE_MISMATCH("wrong price", "price"),
    MAX_BUY_REACHED("maximum limit reached for buying players", "max_buy"),
    ALREADY_OWNED("player is already bought by this user", "already_owned"),
    NOT_OWNED("player wasn't bought by this user", "not_owned"),
    INSUFFICIENT_POINTS("not enough points to buy the player", "insufficient_points"),
    PLAYER_IN_LIVE_MATCH("this player is currently playing a match and can't be bought right now", "live_match"),
    SELL_TOO_SOON("a player can only be sold 48 hours after buying him", "sell_locked");

    public final String message;
    public final String type;

    TransactionFailure(String message, String type) {
        this.message = message;
        this.type = type;
    }
}
