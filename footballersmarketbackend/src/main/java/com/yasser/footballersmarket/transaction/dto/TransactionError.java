package com.yasser.footballersmarket.transaction.dto;

public class TransactionError {
    private String errorType;
    private String errorMsg;
    private Integer playerPrice;

    public TransactionError(){}

    public TransactionError(String errorType, String errorMsg) {
        this.errorType = errorType;
        this.errorMsg = errorMsg;
    }

    public TransactionError(String errorType, String errorMsg, Integer playerPrice) {
        this.errorType = errorType;
        this.errorMsg = errorMsg;
        this.playerPrice = playerPrice;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Integer getPlayerPrice() {
        return playerPrice;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setPlayerPrice(Integer playerPrice) {
        this.playerPrice = playerPrice;
    }
}
