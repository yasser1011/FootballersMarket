package com.yasser.footballersmarket.rapidapi;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.rapidapi.dto.PlayerInfo;
import com.yasser.footballersmarket.rapidapi.dto.PlayerResponse;
import com.yasser.footballersmarket.rapidapi.dto.PlayerStats;
import com.yasser.footballersmarket.rapidapi.dto.PlayersDataSavingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class PlayersDataFetchingProcessingService {
    private final Logger logger = LoggerFactory.getLogger(PlayersDataFetchingProcessingService.class);

    @Autowired
    private PlayersDataSavingProcessingService playersDataSavingProcessingService;

    // hashmap to check if player has many stats from different leagues
    // holds the players from all leagues
    private final HashMap<Long, PlayerLeagueStats> playersMap = new HashMap<>();
    private final HashMap<Long, String> multiplePlayerRecordsInSchedulerMap = new HashMap<>();


    public PlayersDataSavingDto triggerPlayersMapProcess() throws InterruptedException {
        PlayersDataSavingDto playersDataSavingDto = playersDataSavingProcessingService.processPlayersMap(playersMap, multiplePlayerRecordsInSchedulerMap);

        playersMap.clear();
        multiplePlayerRecordsInSchedulerMap.clear();
        return playersDataSavingDto;
    }

    public void addFetchedPlayersList(List<PlayerResponse> fetchedPlayersList){
        for (PlayerResponse player : fetchedPlayersList) {
            PlayerLeagueStats playerTotalLeagueStats = createPlayerStatsEntity(player);

            playersMap.put(playerTotalLeagueStats.getPlayer().getId(), playerTotalLeagueStats);
        }
    }

    private PlayerLeagueStats createPlayerStatsEntity(PlayerResponse playerDto){
        PlayerInfo playerInfo = playerDto.getPlayer();
        Long playerId = playerInfo.getId();

//        String playerName = playerInfo.getName();
        String playerName = "";
        String playerInfoFirstname = playerInfo.getFirstname();
        String playerInfoLastname = playerInfo.getLastname();
        if(playerInfoFirstname == null || playerInfoLastname == null){
            playerName = playerInfo.getName();
        }else{
            playerName = playerInfo.getFirstname() + " " + playerInfo.getLastname();
        }
        PlayerLeagueStats playerLeagueStatsInHashmap = playersMap.get(playerId);

        String playerCurrentRecordClub = playerDto.getStatistics().get(playerDto.getStatistics().size() - 1)
                .getTeam();

        Player playerDbEntity = new Player();

        HashMap<String, Object> playerTotalLeagueStats =
                getPlayerStatsMap(playerDto, playerLeagueStatsInHashmap, playerCurrentRecordClub);

        playerDbEntity.setId(playerId);
        playerDbEntity.setRapidApiClub(playerCurrentRecordClub);
        playerDbEntity.setName(playerName);
        playerDbEntity.setDateOfBirth(playerInfo.getDateOfBirth());
        playerDbEntity.setNationality(playerInfo.getNationality());
        playerDbEntity.setCurrentlyInjured(playerInfo.getInjured());
        playerDbEntity.setPosition((String)playerTotalLeagueStats.get("position"));

        Integer totalGames = (Integer)playerTotalLeagueStats.get("totalGames");
        Integer totalGoals = (Integer)playerTotalLeagueStats.get("totalGoals");
        Integer totalAssists = (Integer)playerTotalLeagueStats.get("totalAssists");
        Double rating = (Double)playerTotalLeagueStats.get("rating");

        return new PlayerLeagueStats(totalGoals, totalAssists, totalGames, rating, playerDbEntity);
    }

    private HashMap<String, Object> getPlayerStatsMap(
            PlayerResponse playerDto,
            PlayerLeagueStats playerLeagueStatsInHashmap,
            String playerCurrentRecordClub){
        HashMap<String, Object> playerTotalLeagueStats;

        Long playerId = playerDto.getPlayer().getId();
        String playerName = playerDto.getPlayer().getName();

        // this check just to determine the player has multiple records during scheduler run
        // also some players records are duplicated within one team
        // ex. Cancelo has two records in barcelona team only
        if (playerLeagueStatsInHashmap != null && !playerLeagueStatsInHashmap.getPlayer().getRapidApiClub().equals(playerCurrentRecordClub)){
            String recordInDuplicateMap = multiplePlayerRecordsInSchedulerMap.get(playerId);
            Player playerRecordInHashmap = playerLeagueStatsInHashmap.getPlayer();
            String clubNameInPreviousHashmapRecord = playerRecordInHashmap.getRapidApiClub().toLowerCase();
            if (recordInDuplicateMap != null){
                multiplePlayerRecordsInSchedulerMap.put(playerId, recordInDuplicateMap + "," + playerCurrentRecordClub);
            }else {
                multiplePlayerRecordsInSchedulerMap.put(playerId,
                        playerLeagueStatsInHashmap.getPlayer().getRapidApiClub() + "," + playerCurrentRecordClub);
            }

            logger.info("player already has record in the map from another league player id {} player name {}" +
                            " player club in prev record {}, player club in current record {}",
                    playerId, playerName, clubNameInPreviousHashmapRecord, playerCurrentRecordClub);

            // get sum of his data from multiple leagues
            playerTotalLeagueStats = getPlayerTotalLeagueStats(playerLeagueStatsInHashmap,
                    playerDto.getStatistics());
        }else{

            playerTotalLeagueStats = getPlayerTotalLeagueStats(new PlayerLeagueStats(0,0, 0, 0.0),
                    playerDto.getStatistics());
        }
        return playerTotalLeagueStats;
    }

    // get player totals stats from his already recorded stats from different league
    // and his stats from his current traversing league
    private HashMap<String, Object> getPlayerTotalLeagueStats(PlayerLeagueStats playerRecordedLeagueStats,
                                                              List<PlayerStats> playerLeagueStatsList){
        HashMap<String, Object> playerTotalStats = new HashMap<>();
        int totalCurrentGames = playerRecordedLeagueStats.getTotalNumOfGames(),
                totalCurrentGoals = playerRecordedLeagueStats.getGoals(),
                totalCurrentAssists = playerRecordedLeagueStats.getAssists();
        double avgRating = playerRecordedLeagueStats.getRating();
        for (PlayerStats stat : playerLeagueStatsList) {
            int numOfGames = Optional.ofNullable(stat.getNumOfGames()).orElse(0);

            double DEFAULT_PLAYER_RATING = 6.5;
            double currRating = Optional.ofNullable(stat.getRating())
                    .map(Double::parseDouble)
                    .orElse(DEFAULT_PLAYER_RATING);

            if (numOfGames == 0) {
                currRating = DEFAULT_PLAYER_RATING;
            }

            totalCurrentGames += numOfGames;

            Integer goals = stat.getGoals();
            if (goals != null) {
                totalCurrentGoals += goals;
            }

            Integer assists = stat.getAssists();
            if (assists != null) {
                totalCurrentAssists += assists;
            }

            avgRating = (avgRating > 0) ? (avgRating + currRating) / 2 : currRating;

        }
        playerTotalStats.put("totalGames", totalCurrentGames);
        playerTotalStats.put("totalGoals", totalCurrentGoals);
        playerTotalStats.put("totalAssists", totalCurrentAssists);
        playerTotalStats.put("rating", avgRating);
        playerTotalStats.put("position", playerLeagueStatsList.get(playerLeagueStatsList.size() - 1).getPosition());

        return playerTotalStats;
    }
}
