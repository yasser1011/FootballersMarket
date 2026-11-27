package com.yasser.footballersmarket.playerusercurrentbought;

import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class PlayerUserCurrentBoughtService {
    private final PlayerUserCurrentBoughtRepository playerUserCurrentBoughtRepository;
    private final PlayerService playerService;

    Logger logger = LoggerFactory.getLogger(PlayerUserCurrentBoughtService.class);

    public PlayerUserCurrentBoughtService(PlayerUserCurrentBoughtRepository playerUserCurrentBoughtRepository,
                                          PlayerService playerService) {
        this.playerUserCurrentBoughtRepository = playerUserCurrentBoughtRepository;
        this.playerService = playerService;
    }

    public PlayerUserCurrentBoughtResponse isPlayerCurrentlyBoughtByUser(Long userId, Long playerId){
        logger.info("checking if user id {} has bought player id before {}", userId, playerId);
        PlayerUserCurrentBought playerUserCurrEntity = playerUserCurrentBoughtRepository.
                findOneByUserIdAndPlayerId(userId, playerId);
         if(playerUserCurrEntity != null){
             logger.info("user id {} has bought player id before {}", userId, playerId);
             return new PlayerUserCurrentBoughtResponse(userId, playerId, true,
                     playerUserCurrEntity.getBuyPrice(), playerUserCurrentBoughtRepository.getBoughtAt(userId, playerId));
         }else{
             logger.info("user id {} has not bought player id before {}", userId, playerId);
             return new PlayerUserCurrentBoughtResponse(userId, playerId, false, null);

         }
    }

    public List<PlayerUserCurrentBought> getUserBasicCurrBoughtPlayers(Long userId){
        return playerUserCurrentBoughtRepository.getBasicUserCurrBoughtPlayers(userId);
    }

    public java.time.LocalDateTime getBoughtAt(Long userId, Long playerId){
        return playerUserCurrentBoughtRepository.getBoughtAt(userId, playerId);
    }

    // no transaction: refreshing an externally maintained player calls 365, and the class-level
    // @Transactional would hold a db connection through every one of those calls. the reads and the
    // refresh's write open their own short transactions
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PlayerUserCurrentBoughtDto> getUserBoughtPlayers(Long userId) {
        logger.info("getting user id {} bought players", userId);

        List<PlayerUserCurrentBought> userCurrBoughtPlayers = playerUserCurrentBoughtRepository.getUserCurrBoughtPlayers(userId);
        List<PlayerUserCurrentBoughtDto> userPlayersList = new ArrayList<>();

        for (PlayerUserCurrentBought player : userCurrBoughtPlayers) {
            // the same refresh and price as his details page and the rankings
            PlayerDetailsResDto playerDetailsResDto = playerService.refreshedForListing(
                    playerService.convertPlayerEntityToPlayerDto(player.getPlayer()));
            PlayerUserCurrentBoughtDto playerResponse = new PlayerUserCurrentBoughtDto(playerDetailsResDto.getId(),
                    playerDetailsResDto.getExternalServicePlayerId(), playerDetailsResDto.getName(),
                    playerDetailsResDto.getPhotoUrl(), playerDetailsResDto.getExternalServicePlayerClub(), playerDetailsResDto.getClubPhotoUrl(),
                    playerDetailsResDto.getNationality(), playerDetailsResDto.getDateOfBirth(), playerDetailsResDto.getPosition(),
                    playerDetailsResDto.getUpdatedBy(), playerDetailsResDto.getTotalGoals(), playerDetailsResDto.getTotalAssists(),
                    playerDetailsResDto.getAvgRating(), player.getBuyPrice(), playerDetailsResDto.getPrice(),
                    playerDetailsResDto.getAreClubStatsUpdated(), playerDetailsResDto.getLeagueStats(),
                    player.getBoughtAt(), playerDetailsResDto.getWorldCupStats());
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
