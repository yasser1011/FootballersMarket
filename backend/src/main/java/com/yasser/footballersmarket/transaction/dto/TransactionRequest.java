package com.yasser.footballersmarket.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public record TransactionRequest(
        Long playerId,
        Integer playerSofascoreId,
        @NotNull @Min(1) @Max(2) Integer transactionType,
        @NotNull @Positive Integer price
) {
    @AssertTrue(message = "playerId or playerSofascoreId must be provided")
    @JsonIgnore
    // to avoid adding it with any TransactionRequest object
    public boolean isPlayerIdentifierProvided() {
        return playerId != null || playerSofascoreId != null;
    }

    @AssertTrue(message = "playerId is required when selling")
    @JsonIgnore
    public boolean isPlayerIdProvidedForSell() {
        return transactionType == null || transactionType != 2 || playerId != null;
    }
}
