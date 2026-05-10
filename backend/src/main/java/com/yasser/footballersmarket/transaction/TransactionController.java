package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.transaction.dto.TransactionRequest;
import com.yasser.footballersmarket.transaction.dto.TransactionResponse;
import com.yasser.footballersmarket.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    Logger logger = LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public TransactionResponse makeTransaction(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody TransactionRequest req) {
        logger.info("attempting transaction user id {} player id {} sofascore id {} type {}",
                user.getId(), req.playerId(), req.playerSofascoreId(), req.transactionType());
        return transactionService.makeTransaction(user.getId(), req);
    }
}
