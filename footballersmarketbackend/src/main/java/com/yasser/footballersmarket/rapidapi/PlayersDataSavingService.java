package com.yasser.footballersmarket.rapidapi;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStatsService;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.rapidapi.dto.PlayersDataSavingDto;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.transaction.Transaction;
import com.yasser.footballersmarket.transaction.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
public class PlayersDataSavingService {
    private final Logger logger = LoggerFactory.getLogger(PlayersDataSavingService.class);

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private PlayerUserCurrentBoughtService playerUserCurrentBoughtService;
    @Autowired
    private PlayerLeagueStatsService playerLeagueStatsService;
    @Autowired
    private PlayerChampionsLeagueStatsService playerChampionsLeagueStatsService;
    @Autowired
    private PlayerService playerService;

    public void savePlayersData(PlayersDataSavingDto playersDataSavingDto){
        logger.info("saving players in database from main hashmap");
        List<Integer> playerExtIdsToBeDeleted = playersDataSavingDto.getPlayerExtIdsToBeDeleted();
        if(playerExtIdsToBeDeleted.size() > 0){
            deletePlayersBoughtExternally(playerExtIdsToBeDeleted);
        }

        List<PlayerLeagueStats> playerLeagueStatsList = playersDataSavingDto.getPlayerLeagueStatsList();
        List<Player> playersEntityList = playersDataSavingDto.getPlayersEntityList();
        playerLeagueStatsService.savePlayersLeagueStatsRapidData(playerLeagueStatsList);
        playerService.savePlayersList(playersEntityList);

        // for players who didn't have rapid ids and were bought then got into scheduler
        List<Transaction> playersWithoutIdsNewTransactionList = playersDataSavingDto.getTransactions();
        List<PlayerUserCurrentBought> playersWithoutIdsNewCurrBoughtTransactionList = playersDataSavingDto.getPlayersCurrentlyBoughtByUsers();
        if(playersWithoutIdsNewTransactionList.size() > 0)
            transactionService.saveTransactions(playersWithoutIdsNewTransactionList);
        if(playersWithoutIdsNewCurrBoughtTransactionList.size() > 0)
            playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransactions(playersWithoutIdsNewCurrBoughtTransactionList);
    }

    private void deletePlayersBoughtExternally(List<Integer> playerExtIdsToBeDeleted){
        transactionService.deletePlayersTransactions(playerExtIdsToBeDeleted);
        playerUserCurrentBoughtService.deleteAllPlayersUserCurrBoughtTransactions(playerExtIdsToBeDeleted);
        playerLeagueStatsService.deleteAllPlayersLeagueStatsBySofascoreId(playerExtIdsToBeDeleted);
        playerChampionsLeagueStatsService.deleteAllPlayersClStatsBySofascoreIds(playerExtIdsToBeDeleted);
        playerService.deleteAllPlayersBySofascoreIds(playerExtIdsToBeDeleted);
    }

}
