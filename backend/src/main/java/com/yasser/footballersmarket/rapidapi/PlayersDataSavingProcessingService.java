package com.yasser.footballersmarket.rapidapi;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.rapidapi.dto.PlayersDataSavingDto;
import com.yasser.footballersmarket.scores365.ScoresConfig;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.transaction.Transaction;
import com.yasser.footballersmarket.transaction.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlayersDataSavingProcessingService {
    private final Logger logger = LoggerFactory.getLogger(PlayersDataSavingProcessingService.class);

    @Autowired
    private PlayerService playerService;
    @Autowired
    PlayerLeagueStatsService playerLeagueStatsService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private PlayerUserCurrentBoughtService playerUserCurrBoughtService;
    @Autowired
    private ScoresService scoresService;
    @Autowired
    private ScoresConfig scoresConfig;
    @Autowired
    private Util util;

    @Value("${summer-transfer-market-end}")
    private String summerTransferMarketEndDateStr;
    @Value("${winter-transfer-market-end}")
    private String winterTransferMarketEndDateStr;


    public PlayersDataSavingDto processPlayersMap(HashMap<Long, PlayerLeagueStats> playersMap,
                                                  HashMap<Long, String> multiplePlayerRecordsInSchedulerMap) throws InterruptedException {
        // players which were prev bought from external service search
        List<Integer> playerSfIdsWithoutRapidId = playerService.getPlayersWithoutRapidId();
        HashMap<Integer, Integer> playersWithoutRapidIdMap = util.getPlayerIdsInHashmap(playerSfIdsWithoutRapidId);

        removeOutdatedSchedulerDataPlayers(playersMap);

        // get player ids in main player entity table
        // todo change name getPlayerExternalServiceBasicDetails
        HashMap<Long, PlayerExternalServiceBasicDetails> idsInPlayerTable =
                util.getPlayerIdsWithBasicExternalServiceDetailsInHashmap(playerService.getPlayerExternalServiceBasicDetails());
        // get player ids in main league stats entity table
        HashMap<Long, Integer> idsInPlayerLeagueStatsTable =
                util.getPlayerIdsInHashmap(playerLeagueStatsService.getRapidDataPlayerLeagueStatsIds());

        PlayersDataSavingDto playersDataSavingDto = new PlayersDataSavingDto();

        boolean isCurrDateEndOfTransferMarket = util.isCurrDateEndOfTransferMarket(winterTransferMarketEndDateStr, summerTransferMarketEndDateStr);
        for (Map.Entry<Long, PlayerLeagueStats> player : playersMap.entrySet()) {
            Long playerId = player.getKey();

            PlayerLeagueStats playerLeagueStats = player.getValue();

            PlayerExternalServiceBasicDetails isPlayerIdFoundInDb = idsInPlayerTable.get(playerId);
            // check if player record is new data -> fetch data from external service
            if (isCurrDateEndOfTransferMarket || isPlayerIdFoundInDb == null){
                PlayerLeagueStats playerLeagueStatsUpdated = fetchPlayerDataFromExternalService(playerLeagueStats);
                if(playerLeagueStatsUpdated == null){
                    logger.info("player not found in external search {}", playerId);
                    continue;
                }

                String isPlayerFoundInMultipleRecordsMap = multiplePlayerRecordsInSchedulerMap.get(playerId);
                if (isPlayerFoundInMultipleRecordsMap != null){
                    setPlayerCorrectSchedulerClubName(playerLeagueStatsUpdated, isPlayerFoundInMultipleRecordsMap);
                }
                playerLeagueStats = playerLeagueStatsUpdated;
                // for rate limiting
                Thread.sleep(50);
            }

            Integer playerSofascoreId = playerLeagueStats.getPlayer().getSofascoreId();

            // check if a player was bought from external service search and then was available in scheduler runs after moving to a new team for ex
            Integer isPlayerFoundInPlayersWithoutRapidIdMap = playersWithoutRapidIdMap.get(playerSofascoreId);
            if(isPlayerFoundInPlayersWithoutRapidIdMap != null){
                removePlayerOldTransactionsOutsideOfScheduler(playerId, playerSofascoreId, playersDataSavingDto);
            }

            // fill playerLeagueStatsList and playersEntityList to be saved in database properly
            addPlayerToDataSavingLists(playerId, playerLeagueStats, idsInPlayerTable, idsInPlayerLeagueStatsTable, playersDataSavingDto);
        }

        return playersDataSavingDto;
    }

    private void addPlayerToDataSavingLists(Long playerId, PlayerLeagueStats leagueStat,
                                            HashMap<Long, PlayerExternalServiceBasicDetails> idsInPlayerTable,
                                            HashMap<Long, Integer> idsInPlayerLeagueStatsTable, PlayersDataSavingDto playersDataSavingDto) {
        PlayerExternalServiceBasicDetails playerFoundInPlayerTable =  idsInPlayerTable.get(playerId);
        Integer playerFoundInPlayerLeagueStatsTable =  idsInPlayerLeagueStatsTable.get(playerId);

        if(playerFoundInPlayerTable != null && playerFoundInPlayerLeagueStatsTable != null){
            // appears in both tables just update in league stats table
            PlayerLeagueStats playerLeagueStats = new PlayerLeagueStats(playerId, leagueStat.getGoals(),
                    leagueStat.getAssists(), leagueStat.getRating(), leagueStat.getTotalNumOfGames());
            playersDataSavingDto.addPlayerLeagueStatsToLeagueStatsList(playerLeagueStats);

        }else if(playerFoundInPlayerTable != null){
            // found in players main table but not in league stats table so attach a new record in stats table to his main player obj
            Player playerToSave = leagueStat.getPlayer();
            playerToSave.setRapidApiClub(playerFoundInPlayerTable.getRapidApiClub());
            playerToSave.setSofascoreClub(playerFoundInPlayerTable.getSofascoreClub());
            playerToSave.setSofascoreId(playerFoundInPlayerTable.getSofascoreId());
            playerToSave.setPhotoUrl(playerFoundInPlayerTable.getPhotoUrl());
            playerToSave.setClubPhotoUrl(playerFoundInPlayerTable.getClubPhotoUrl());
            playerToSave.setLeagueStats(leagueStat);
            playersDataSavingDto.addPlayerToPlayersList(playerToSave);
        }else
            // add stats in both tables
            playersDataSavingDto.addPlayerLeagueStatsToLeagueStatsList(leagueStat);
    }

    private void removePlayerOldTransactionsOutsideOfScheduler(Long playerId, Integer playerSofascoreId, PlayersDataSavingDto playersDataSavingDto) {

        // create same transactions but with new player id and add them
        List<Transaction> playerTransactions = transactionService.getPlayerTransactions(playerSofascoreId);
        List<PlayerUserCurrentBought> playerUserCurrBoughtTransactions =
                playerUserCurrBoughtService.getPlayerUserCurrBoughtTransactions(playerSofascoreId);
        playerTransactions.forEach(transaction -> {
            transaction.setPlayer(null);
            transaction.setPlayerId(playerId);
            playersDataSavingDto.addTransactionToList(transaction);
        });
        playerUserCurrBoughtTransactions.forEach(playerCurrBuyTransaction -> {
            playerCurrBuyTransaction.setPlayer(null);
            playerCurrBuyTransaction.setPlayerId(playerId);
            playersDataSavingDto.addPlayerToCurrentBoughtPlayersList(playerCurrBuyTransaction);
        });
        playersDataSavingDto.addExtIdsToExtPlayerIdsList(playerSofascoreId);

    }

    private PlayerLeagueStats setPlayerCorrectSchedulerClubName(PlayerLeagueStats playerLeagueStats, String multipleClubNamesRecord){
        Player playerUpdatedDbEntity = playerLeagueStats.getPlayer();
        String [] clubsFound = multipleClubNamesRecord.split(",");
        String schedulerCorrectClubName = util.determinePlayerSchedulerMultipleRecordsClub(clubsFound, playerUpdatedDbEntity.getSofascoreClub());
        playerUpdatedDbEntity.setRapidApiClub(schedulerCorrectClubName);
        playerLeagueStats.setPlayer(playerUpdatedDbEntity);
        return playerLeagueStats;
    }

    private void removeOutdatedSchedulerDataPlayers(HashMap<Long, PlayerLeagueStats> playersMap){
        List<PlayerBasic> sofascorePlayers = playerService.getPlayersUpdatedBySofascore();
        sofascorePlayers.forEach(sofascorePlayer -> playersMap.remove(sofascorePlayer.getId()));
    }

    private PlayerLeagueStats fetchPlayerDataFromExternalService(PlayerLeagueStats playerStatsEntity) throws InterruptedException {
        Player playerDbEntity = playerStatsEntity.getPlayer();
        String playerName = playerDbEntity.getName();
        String simplifiedPlayerName = util.getFirstTwoWordsFromName(playerName);

        SearchPlayerEntity closestPlayer = scoresService.searchClosestPlayerByName(simplifiedPlayerName);
        if (closestPlayer == null) {
            return null;
        }
        logger.info("player from external search, player id {} player name {}", playerDbEntity.getId(), playerName);
        String teamName = closestPlayer.getTeamName();

        Integer sofascoreId = closestPlayer.getId();
        playerDbEntity.setSofascoreId(sofascoreId);
        playerDbEntity.setSofascoreClub(teamName);
        playerDbEntity.setClubPhotoUrl(scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresClubsImageEndPoint() + closestPlayer.getTeamId());
        playerDbEntity.setPhotoUrl(scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresPlayersImageEndPoint() + sofascoreId);

        playerStatsEntity.setPlayer(playerDbEntity);

        return playerStatsEntity;
    }
}
