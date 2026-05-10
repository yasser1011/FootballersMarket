package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.transaction.dto.TransactionError;
import com.yasser.footballersmarket.transaction.dto.TransactionException;
import com.yasser.footballersmarket.transaction.dto.TransactionFailure;
import com.yasser.footballersmarket.transaction.dto.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.EntityNotFoundException;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = TransactionController.class)
public class TransactionExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(TransactionExceptionHandler.class);

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<TransactionResponse> handleTransaction(TransactionException e) {
        logger.warn("transaction rejected: {}", e.getTransactionFailure());
        String type = e.getTransactionFailure() == TransactionFailure.PRICE_MISMATCH ? "price" : "generic";
        TransactionError err = new TransactionError(type, e.getMessage(), e.getPlayerPrice());
        return ResponseEntity.badRequest().body(new TransactionResponse(-1, err));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<TransactionResponse> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("validation failed: {}", msg);
        return ResponseEntity.badRequest().body(
                new TransactionResponse(-1, new TransactionError("validation", msg, null)));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<TransactionResponse> handleNotFound(EntityNotFoundException e) {
        logger.warn("entity not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new TransactionResponse(-1, new TransactionError("notfound", e.getMessage(), null)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TransactionResponse> handleUnexpected(Exception e) {
        logger.error("unexpected error in transaction flow", e);
        return ResponseEntity.internalServerError().body(
                new TransactionResponse(-1, new TransactionError("generic", "unexpected error", null)));
    }
}
