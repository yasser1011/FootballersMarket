package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.fixture.FixtureResultConstants;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityNotFoundException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final PlayerService playerService;
    private final SofascoreService sofascoreService;
    private final PlayerUserCurrentBoughtService playerUserCurrentBoughtService;
    private final UserService userService;
    private final PriceStrategy priceStrategy;
    private final FixtureRepository fixtureRepository;
    // the buy/sell write, see makeTransaction
    private final TransactionTemplate buyOrSellTransaction;

    // a freshly bought player can't be sold until this many hours pass, so a player can't be
    // bought right before his match and dumped right after to harvest the in-match price spike
    private static final long SELL_LOCK_HOURS = 48;

    // a league fixture that kicked off longer ago than this no longer counts as live, whatever its
    // status says: a match with extra time and penalties fits inside it
    static final Duration LIVE_MATCH_WINDOW = Duration.ofHours(3);

    Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository, PlayerService playerService,
                              SofascoreService sofascoreService,
                              PlayerUserCurrentBoughtService playerUserCurrentBoughtService,
                              UserService userService,
                              PriceStrategy priceStrategy,
                              FixtureRepository fixtureRepository,
                              PlatformTransactionManager transactionManager) {
        this.transactionRepository = transactionRepository;
        this.playerService = playerService;
        this.sofascoreService = sofascoreService;
        this.playerUserCurrentBoughtService = playerUserCurrentBoughtService;
        this.userService = userService;
        this.priceStrategy = priceStrategy;
        this.fixtureRepository = fixtureRepository;
        this.buyOrSellTransaction = new TransactionTemplate(transactionManager);
        // the budget the old @Transactional(timeout = 10) gave the whole call, now for the db work only
        this.buyOrSellTransaction.setTimeout(10);
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

    // two steps, deliberately NOT one transaction:
    //  1. resolve the player and check the price -- no transaction. this may call 365 (a player not
    //     in our db, or an externally refreshed one whose data is stale), and an http call inside the
    //     transaction would hold a db connection and eat the 10s budget while waiting on the network
    //  2. the buy/sell itself -- one short transaction (user lock, holdings, transaction row), db only.
    //     a TransactionTemplate, because calling a @Transactional method of this class from here
    //     would bypass the proxy and run with no transaction at all
    public TransactionResponse makeTransaction(Long userId, TransactionRequest req) {
        PlayerDetailsResDto playerDto = playerService.resolveForTransaction(req.playerId(), req.playerSofascoreId());

        if(!playerDto.getPrice().equals(req.price())){
            throw new TransactionException(TransactionFailure.PRICE_MISMATCH, playerDto.getPrice());
        }
        return buyOrSellTransaction.execute(status -> applyBuyOrSell(userId, req, playerDto));
    }

    public TransactionResponse applyBuyOrSell(Long userId, TransactionRequest req, PlayerDetailsResDto playerDto) {
        final int BUY = 1;
        final int MAX_BUY_SIZE = 4;

        User user = userService.findByIdForUpdate(userId);
        boolean worldCupMode = priceStrategy.isWorldCupMode();

        List<PlayerUserCurrentBought> ownedPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(userId);
        boolean alreadyOwned = ownedPlayers.stream()
                .anyMatch(p -> Objects.equals(p.getPlayerId(), playerDto.getId()));

        if (req.transactionType() == BUY) {
            if (ownedPlayers.size() >= MAX_BUY_SIZE) throw new TransactionException(TransactionFailure.MAX_BUY_REACHED);
            if (alreadyOwned)                        throw new TransactionException(TransactionFailure.ALREADY_OWNED);
            if (user.getPoints() < req.price())      throw new TransactionException(TransactionFailure.INSUFFICIENT_POINTS);
            if (isInLiveMatch(playerDto.getId()))
                throw new TransactionException(TransactionFailure.PLAYER_IN_LIVE_MATCH);

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
            PlayerUserCurrentBought holding = new PlayerUserCurrentBought(userId, playerDto.getId(), req.price());
            holding.setBoughtAt(LocalDateTime.now());
            playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(holding);
        } else {
            if (!alreadyOwned) throw new TransactionException(TransactionFailure.NOT_OWNED);
            if (isInLiveMatch(playerDto.getId()))
                throw new TransactionException(TransactionFailure.PLAYER_IN_LIVE_MATCH);
            // a player can't be sold within the lock window after buying him
            if (worldCupMode) {
                LocalDateTime boughtAt = playerUserCurrentBoughtService.getBoughtAt(userId, playerDto.getId());
                if (boughtAt != null && LocalDateTime.now().isBefore(boughtAt.plusHours(SELL_LOCK_HOURS)))
                    throw new TransactionException(TransactionFailure.SELL_TOO_SOON);
            }
            user.setPoints(user.getPoints() + req.price());
            playerUserCurrentBoughtService.deletePlayerUserCurrBoughtTransaction(userId, playerDto.getId());
        }

        Transaction saved = transactionRepository.save(new Transaction(
                req.transactionType(), req.price(), LocalDateTime.now(), userId, playerDto.getId()));

        return new TransactionResponse(0, saved.getId(), saved.getTimestamp(), null);
    }

    // no buy or sell while his match is being played: the price only moves after it, so trading
    // mid-match would be trading on a result already known. db only, safe inside the transaction.
    // a player not in our db yet (null id) has no fixtures of ours, so he reads as not playing
    private boolean isInLiveMatch(Long playerId) {
        if (playerId == null) return false;
        Instant now = Instant.now();
        return fixtureRepository.isPlayersClubPlayingNow(playerId, now, now.minus(LIVE_MATCH_WINDOW),
                FixtureResultConstants.EXCLUDED_FROM_RESULTS_POLL);
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
