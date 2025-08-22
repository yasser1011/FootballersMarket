package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.transaction.dto.TransactionError;
import com.yasser.footballersmarket.transaction.dto.TransactionRequest;
import com.yasser.footballersmarket.transaction.dto.TransactionResponse;
import com.yasser.footballersmarket.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    Logger logger = LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping()
    public TransactionResponse makePlayerTransaction(@RequestBody TransactionRequest transactionRequest){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = user.getId();
        Integer userPoints = user.getPoints();

        Long playerId = transactionRequest.getPlayerId();
        Integer playerSofascoreId = transactionRequest.getPlayerSofascoreId();
        Integer transactionType = transactionRequest.getTransactionType();
        logger.info("attempting to make a transaction user id {} player id {} sofascore id {} transaction type {}",
                userId, playerId, playerSofascoreId, transactionType);
        try {
            TransactionResponse transactionRes = transactionService.makePlayerTransaction(userId, userPoints, playerId,
                    playerSofascoreId, transactionType,
                    transactionRequest.getPrice());
            return transactionRes;
        }catch (Exception e){
            logger.error("error making transaction user id {} player id {} sofascore id {} transaction type {} error {}",
                    userId, playerId, playerSofascoreId, transactionType, e.getMessage());

            TransactionError transactionError = new TransactionError();
            transactionError.setErrorType("generic");
            transactionError.setErrorMsg(e.getMessage());
            return new TransactionResponse(-1, transactionError);
        }
    }

}
