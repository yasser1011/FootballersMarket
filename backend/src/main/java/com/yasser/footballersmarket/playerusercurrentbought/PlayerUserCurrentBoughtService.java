package com.yasser.footballersmarket.playerusercurrentbought;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class PlayerUserCurrentBoughtService {
    private final PlayerUserCurrentBoughtRepository playerUserCurrentBoughtRepository;
    private final PlayerService playerService;
    private final ScoresService scoresService;

    Logger logger = LoggerFactory.getLogger(PlayerUserCurrentBoughtService.class);

    public PlayerUserCurrentBoughtService(PlayerUserCurrentBoughtRepository playerUserCurrentBoughtRepository,
                                          PlayerService playerService, ScoresService scoresService) {
        this.playerUserCurrentBoughtRepository = playerUserCurrentBoughtRepository;
        this.playerService = playerService;
        this.scoresService = scoresService;
    }

    public PlayerUserCurrentBoughtResponse isPlayerCurrentlyBoughtByUser(Long userId, Long playerId){
        logger.info("checking if user id {} has bought player id before {}", userId, playerId);
        PlayerUserCurrentBought playerUserCurrEntity = playerUserCurrentBoughtRepository.
                findOneByUserIdAndPlayerId(userId, playerId);
         if(playerUserCurrEntity != null){
             logger.info("user id {} has bought player id before {}", userId, playerId);
             return new PlayerUserCurrentBoughtResponse(userId, playerId, true,
                     playerUserCurrEntity.getBuyPrice());
         }else{
             logger.info("user id {} has not bought player id before {}", userId, playerId);
             return new PlayerUserCurrentBoughtResponse(userId, playerId, false, null);

         }
    }

    public List<PlayerUserCurrentBought> getUserBasicCurrBoughtPlayers(Long userId){
        return playerUserCurrentBoughtRepository.getBasicUserCurrBoughtPlayers(userId);
    }

    public List<PlayerUserCurrentBoughtDto> getUserBoughtPlayers(Long userId) throws InterruptedException {
        logger.info("getting user id {} bought players", userId);

        List<PlayerUserCurrentBought> userCurrBoughtPlayers = playerUserCurrentBoughtRepository.getUserCurrBoughtPlayers(userId);
        List<PlayerUserCurrentBoughtDto> userPlayersList = new ArrayList<>();

        for (PlayerUserCurrentBought player : userCurrBoughtPlayers) {
            Player playerEntity = player.getPlayer();
            PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(playerEntity);
            if (!playerDetailsResDto.getAreClubStatsUpdated() ||
                    (playerEntity.getUpdatedBy() != null && playerEntity.getUpdatedBy().contains("sofascore"))){
                // fetch updated stats and update in db
                // and set current entity league stats instead of creating new entity to avoid
                // A different object with the same identifier value was already associated with the session
                PlayerDetailsDto playerDetailsDto = scoresService.fetchPlayerEntityFromExternalService(playerEntity.getSofascoreId());
                PlayerLeagueStats playerEntityLeagueStats = playerEntity.getLeagueStats();
                playerEntityLeagueStats.setRating(playerDetailsDto.getLeagueStats().getRating());
                playerEntityLeagueStats.setGoals(playerDetailsDto.getLeagueStats().getGoals());
                playerEntityLeagueStats.setAssists(playerDetailsDto.getLeagueStats().getAssists());
                playerEntityLeagueStats.setTotalNumOfGames(playerDetailsDto.getLeagueStats().getAppearances());
                playerService.updatePlayerLeagueStatsDetails(playerEntity.getId(), playerEntityLeagueStats);
            }
            playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(playerEntity);
            PlayerUserCurrentBoughtDto playerResponse = new PlayerUserCurrentBoughtDto(playerDetailsResDto.getId(),
                    playerDetailsResDto.getExternalServicePlayerId(), playerDetailsResDto.getName(),
                    playerDetailsResDto.getPhotoUrl(), playerDetailsResDto.getExternalServicePlayerClub(), playerDetailsResDto.getClubPhotoUrl(),
                    playerDetailsResDto.getNationality(), playerDetailsResDto.getDateOfBirth(), playerDetailsResDto.getPosition(),
                    playerDetailsResDto.getUpdatedBy(), playerDetailsResDto.getTotalGoals(), playerDetailsResDto.getTotalAssists(),
                    playerDetailsResDto.getAvgRating(), player.getBuyPrice(), playerDetailsResDto.getPrice(),
                    playerDetailsResDto.getAreClubStatsUpdated(), playerDetailsResDto.getLeagueStats());
            userPlayersList.add(playerResponse);
        }
        // sort list based on price desc
        userPlayersList.sort((o1, o2) -> o2.getPrice() - o1.getPrice());
        return userPlayersList;
    }

    public List<PlayerUserCurrentBought> getPlayerUserCurrBoughtTransactions(Integer playerSofascoreId){
        return playerUserCurrentBoughtRepository.getPlayerCurrBoughtTransactions(playerSofascoreId);
    }

    public void deletePlayerUserCurrBoughtTransactions(Integer sofascoreId){
        playerUserCurrentBoughtRepository.deletePlayerCurrBoughtTransactions(sofascoreId);
    }

    public void deleteAllPlayersUserCurrBoughtTransactions(List<Integer> sofascoreIds){
        playerUserCurrentBoughtRepository.deleteAllPlayersCurrBoughtTransactions(sofascoreIds);
    }

    public void savePlayerUserCurrBoughtTransaction(PlayerUserCurrentBought playerUserCurrBoughtTransaction){
        playerUserCurrentBoughtRepository.save(playerUserCurrBoughtTransaction);
    }

    public void savePlayerUserCurrBoughtTransactions(List<PlayerUserCurrentBought> playerUserCurrBoughtList){
        playerUserCurrentBoughtRepository.saveAll(playerUserCurrBoughtList);
    }

    public void deletePlayerUserCurrBoughtTransaction(Long userId, Long playerId){
        playerUserCurrentBoughtRepository.deletePlayerCurrBoughtTransaction(userId, playerId);
    }

    public void deleteAll(){
        playerUserCurrentBoughtRepository.deleteAll();
    }
}
