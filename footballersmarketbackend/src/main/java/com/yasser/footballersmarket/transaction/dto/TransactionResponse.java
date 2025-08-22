package com.yasser.footballersmarket.transaction.dto;

import java.time.LocalDateTime;

public class TransactionResponse {
    private Integer status;
    private Long transactionId;
    private LocalDateTime timestamp;
    private TransactionError error;

    public TransactionResponse(){}
    public TransactionResponse(Integer status, TransactionError error) {
        this.status = status;
        this.error = error;
    }

    public TransactionResponse(Integer status, Long transactionId, LocalDateTime timestamp, TransactionError error) {
        this.status = status;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.error = error;
    }

    public Integer getStatus() {
        return status;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionError getError() {
        return error;
    }
}
