package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.transaction.dto.*;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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

    private PlayerDetailsResDto resolvePlayer(TransactionRequest req) {
        if (req.playerId() != null) {
            return playerService.getPlayerDetailsByInternalId(req.playerId());
        }
        // in normal condition db lookup by external id is not needed because of player id is not provided it would mean player is not in db
        // but in case of having a race condition when two users checking the same search player
        // and one of them buys it before the other. the second user would not have internal player id so need to check by external id
        // and not rely on frontend providing correct data entirely
        PlayerDetailsResDto inDb = playerService.getPlayerDetailsByExternalId(req.playerSofascoreId());
        if(inDb != null)
            return inDb;
        else{
            PlayerDetailsDto playerResponse = scoresService.fetchPlayerEntityFromExternalService(req.playerSofascoreId());
            if (playerResponse == null) {
                throw new EntityNotFoundException("player not found in external service " + req.playerSofascoreId());
            }
            return scoresService.convertDtoToPlayerDetailsDto(req.playerSofascoreId(), playerResponse, playerResponse.getRecentMatches());
        }
    }

    @Transactional(timeout = 10)
    public TransactionResponse makeTransaction(Long userId, TransactionRequest req) {
        PlayerDetailsResDto playerDto = resolvePlayer(req);

        if(!playerDto.getPrice().equals(req.price())){
            throw new TransactionException(TransactionFailure.PRICE_MISMATCH, playerDto.getPrice());
        }
        return applyBuyOrSell(userId, req, playerDto);
    }

    public TransactionResponse applyBuyOrSell(Long userId, TransactionRequest req, PlayerDetailsResDto playerDto) {
        final int BUY = 1;
        final int MAX_BUY_SIZE = 4;

        User user = userService.findByIdForUpdate(userId);

        List<PlayerUserCurrentBought> ownedPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(userId);
        boolean alreadyOwned = ownedPlayers.stream()
                .anyMatch(p -> Objects.equals(p.getPlayerId(), playerDto.getId()));

        if (req.transactionType() == BUY) {
            if (ownedPlayers.size() >= MAX_BUY_SIZE) throw new TransactionException(TransactionFailure.MAX_BUY_REACHED);
            if (alreadyOwned)                        throw new TransactionException(TransactionFailure.ALREADY_OWNED);
            if (user.getPoints() < req.price())      throw new TransactionException(TransactionFailure.INSUFFICIENT_POINTS);

            // Persist player only after validations pass so a failed buy doesn't leave an orphan row.
            // Joins this transaction -> rolls back together if anything below throws.
            if (playerDto.getId() == null) {
                Player toInsert = playerService.convertPlayerDtoToPlayerEntity(playerDto);
                toInsert.setUpdatedBy("sofascore_buy");

                String uuidStr = String.format("%010d",
                        new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
                toInsert.setId(Long.parseLong(uuidStr.substring(uuidStr.length() - 10)));
                playerService.savePlayer(toInsert);
                playerDto.setId(toInsert.getId());
            }

            user.setPoints(user.getPoints() - req.price());
            playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(
                    new PlayerUserCurrentBought(userId, playerDto.getId(), req.price()));
        } else {
            if (!alreadyOwned) throw new TransactionException(TransactionFailure.NOT_OWNED);
            user.setPoints(user.getPoints() + req.price());
            playerUserCurrentBoughtService.deletePlayerUserCurrBoughtTransaction(userId, playerDto.getId());
        }

        Transaction saved = transactionRepository.save(new Transaction(
                req.transactionType(), req.price(), LocalDateTime.now(), userId, playerDto.getId()));

        return new TransactionResponse(0, saved.getId(), saved.getTimestamp(), null);
    }
    public Page<Transaction> getTransactions(int pageNum){
        // page num starts with 0
        Pageable pageData = PageRequest.of(pageNum, 50);
        return transactionRepository.getTransactionsPage(pageData);
    }

    public void deleteAll(){
        transactionRepository.deleteAll();
    }
}
