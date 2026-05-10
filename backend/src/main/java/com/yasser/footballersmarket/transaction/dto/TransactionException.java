package com.yasser.footballersmarket.transaction.dto;

public class TransactionException extends RuntimeException {
    private final TransactionFailure transactionFailure;
    private final Integer playerPrice;

    public TransactionException(TransactionFailure transactionFailure, Integer playerPrice) {
        super(transactionFailure.message);
        this.transactionFailure = transactionFailure;
        this.playerPrice = playerPrice;
    }

    public TransactionException(TransactionFailure transactionFailure) {
        this(transactionFailure, null);
    }

    public TransactionFailure getTransactionFailure() {
        return transactionFailure;
    }

    public Integer getPlayerPrice() {
        return playerPrice;
    }
}
