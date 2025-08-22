package com.yasser.footballersmarket.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.transaction.dto.TransactionError;
import com.yasser.footballersmarket.transaction.dto.TransactionResponse;
import com.yasser.footballersmarket.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final PlayerService playerService;
    private final SofascoreService sofascoreService;
    private final ScoresService scoresService;
    private final PlayerUserCurrentBoughtService playerUserCurrentBoughtService;
    private final UserService userService;

    Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository, PlayerService playerService,
                              SofascoreService sofascoreService,
                              PlayerUserCurrentBoughtService playerUserCurrentBoughtService,
                              UserService userService, ScoresService scoresService) {
        this.transactionRepository = transactionRepository;
        this.playerService = playerService;
        this.sofascoreService = sofascoreService;
        this.scoresService = scoresService;
        this.playerUserCurrentBoughtService = playerUserCurrentBoughtService;
        this.userService = userService;
    }

    public List<Transaction> getPlayerTransactions(Integer playerSofascoreId){
        return transactionRepository.getPlayerTransactions(playerSofascoreId);
    }

    @Transactional
    public void deletePlayerTransactions(Integer sofascoreId){
        transactionRepository.deletePlayerTransactions(sofascoreId);
    }

    @Transactional
    public void deletePlayersTransactions(List<Integer> sofascoreIds){
        transactionRepository.deleteAllPlayersTransactions(sofascoreIds);
    }

    public void saveTransactions(List<Transaction> playerTransactions){
        transactionRepository.saveAll(playerTransactions);
    }

    @Transactional
    public TransactionResponse makePlayerTransaction(Long userId, Integer userPoints, Long playerId, Integer playerSofascoreId,
                                                     Integer transactionType, Integer price) throws JsonProcessingException {
        logger.info("attempting transaction by user user id {} player id {} sofascore id {} transaction type {} price {}",
                userId, playerId, playerSofascoreId, transactionType, price);
        // transaction type 1 = buy, 2 = sell
        if(transactionType != 1 && transactionType != 2){
            logger.error("wrong transaction type {}", transactionType);
            return new TransactionResponse(-1, new TransactionError("generic", "wrong transaction type"));
        }

        if(playerId == null && transactionType == 2){
            logger.error("wrong transaction type {} can't sell a player that is not in database with no player id", transactionType);
            return new TransactionResponse(-1, new TransactionError("generic", "player id not provided"));
        }

        try {
            // this is throwing exception if player not found so if it passes means player details found
            PlayerDetailsResDto playerDetails = getPlayerDetails(playerId, playerSofascoreId);
            if(playerDetails.getId() == null){
                logger.info("player returned with no id");
                return new TransactionResponse(-1, new TransactionError("generic", "player returned with no id"));
            }
            if (!playerDetails.getPrice().equals(price)){
                // for price validation must provide price from frontend to avoid having specific price in front that doesn't match the DB
                // can't rely on price provided from client only without DB validation
                logger.error("price provided in request is wrong, price provided {} actual price {}", price, playerDetails.getPrice());
                return new TransactionResponse(-1, new TransactionError("price", "wrong price", playerDetails.getPrice()));
            }

            List<PlayerUserCurrentBought> userBasicCurrBoughtPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(userId);

            int MAX_BUY_SIZE = 7;
            if(userBasicCurrBoughtPlayers.size() == MAX_BUY_SIZE){
                logger.info("user cannot buy more than 7 players");
                return new TransactionResponse(-1, new TransactionError("generic", "maximum limit reached for buying players"));
            }

            boolean isPlayerCurrBoughtByUser = false;
            for (PlayerUserCurrentBought userBasicCurrBoughtPlayer : userBasicCurrBoughtPlayers) {
                Long transactionPlayerId = userBasicCurrBoughtPlayer.getPlayerId();
                if (transactionPlayerId.equals(playerId)) {
                    isPlayerCurrBoughtByUser = true;
                    break;
                }
            }
            if(transactionType == 1 && isPlayerCurrBoughtByUser){
                logger.info("wrong transaction type provided player is already bought by user user id {} player id {} sofascore id {}", userId, playerId, playerSofascoreId);
                return new TransactionResponse(-1, new TransactionError("generic", "player is already bought by this user"));
            }else if(transactionType == 2 && !isPlayerCurrBoughtByUser){
                logger.info("wrong transaction type provided player is not bought by user user id {} player id {} sofascore id {}", userId, playerId, playerSofascoreId);
                return new TransactionResponse(-1, new TransactionError("generic", "player wasn't bought by this user"));
            }

            // check user points balance
            if(transactionType == 1 && userPoints < price){
                logger.info("not enough points to buy the player id {} sofascore id {} user points {} player price {}",
                        playerId, playerSofascoreId, userPoints, price);
                return new TransactionResponse(-1, new TransactionError("generic", "not enough points to buy the player"));
            }

            int userNewPoints;
            if(transactionType == 1)
                userNewPoints = userPoints - price;
            else
                userNewPoints = userPoints + price;

            Transaction newTransaction = new Transaction(transactionType, price, LocalDateTime.now(), userId, playerDetails.getId());
            logger.info("updating new user point user id {} new points {}", userId, userNewPoints);
            userService.updateUserPoints(userId, userNewPoints);

            logger.info("saving transaction player id {} user id {} price {}", playerDetails.getId(), userId, price);
            Transaction savedTransaction = transactionRepository.save(newTransaction);
            if(transactionType == 1){
                logger.info("adding player to current bought players user id {} player id {}", userId, playerDetails.getId());
                PlayerUserCurrentBought currBoughtTransaction = new PlayerUserCurrentBought(userId, playerDetails.getId(), price);
                playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction);
            }else {
                logger.info("deleting player from current bought players user id {} player id {}", userId, playerDetails.getId());
                playerUserCurrentBoughtService.deletePlayerUserCurrBoughtTransaction(userId, playerDetails.getId());
            }
            // must return object with error message if price changed
            // or if user has no enough points
            // or if user id doesn't match token user
            return new TransactionResponse(0, savedTransaction.getId(),
                    savedTransaction.getTimestamp(), null);
        }catch (Exception e){
            logger.error("error making transaction {} user id {} player id {} sofascore id {}", e.getMessage(), userId, playerId, playerSofascoreId);
            return new TransactionResponse(-1, new TransactionError("generic", e.getMessage()));
        }

    }
    public Page<Transaction> getTransactions(int pageNum){
        // page num starts with 0
        Pageable pageData = PageRequest.of(pageNum, 50);
        return transactionRepository.getTransactionsPage(pageData);
    }


    private PlayerDetailsResDto getPlayerDetails(Long playerId, Integer playerSofascoreId) throws JsonProcessingException {
        logger.info("getting player details for transaction player id {} sofascore id {}", playerId, playerSofascoreId);
        if(playerId == null && playerSofascoreId == null){
            logger.error("player ids not provided");
            throw new IllegalStateException("no player ids provided");
        }
        Player player;
        PlayerDetailsResDto playerDetailsResDto;
        if(playerId == null){
            logger.info("getting player in sofascore sofascore id {}", playerSofascoreId);
            PlayerDetailsResDto playerInDbBySofascore = playerService.getPlayerBySofascoreId(playerSofascoreId);
            // to make sure no illegal state in app -> if player in db, playerId should not be null
            if (playerInDbBySofascore != null){
                logger.error("player found in db while no player id provided sofascore id {}", playerSofascoreId);
                throw new IllegalStateException("player found in db while no player id provided");
            }
            // player not found in db get from sofascore service
            playerDetailsResDto = scoresService.getPlayerEntityFromExternalService(playerSofascoreId);
            Player player1 = playerService.convertPlayerDtoToPlayerEntity(playerDetailsResDto);
            String generateUUIDNo2 = String.format("%010d", new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
            Long uniqueRandomId = Long.parseLong(generateUUIDNo2.substring(generateUUIDNo2.length() - 10));
            player1.setId(uniqueRandomId);
            player1.setUpdatedBy("sofascore_buy");
            playerDetailsResDto.setId(uniqueRandomId);
            playerDetailsResDto.setUpdatedBy("sofascore_buy");

            logger.info("adding player to player table with random id {} sofascore id {}", uniqueRandomId, playerSofascoreId);
            playerService.savePlayer(player1);
        }else{
            logger.info("getting player from database player id {} sofascore id {}", playerId,playerSofascoreId);
            playerDetailsResDto = playerService.getPlayerByIdAndSofascoreId(playerId, playerSofascoreId);
        }
        if(playerDetailsResDto == null){
            logger.error("error retrieving player details player id {} sofascore id {}", playerId, playerSofascoreId);
            throw new IllegalStateException("couldn't get player data");
        }
        return playerDetailsResDto;
    }

    public void deleteAll(){
        transactionRepository.deleteAll();
    }
}
