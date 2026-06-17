package com.yasser.footballersmarket.playerusercurrentbought;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.pricing.PriceStrategy;
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
    private final PriceStrategy priceStrategy;

    Logger logger = LoggerFactory.getLogger(PlayerUserCurrentBoughtService.class);

    public PlayerUserCurrentBoughtService(PlayerUserCurrentBoughtRepository playerUserCurrentBoughtRepository,
                                          PlayerService playerService, ScoresService scoresService,
                                          PriceStrategy priceStrategy) {
        this.playerUserCurrentBoughtRepository = playerUserCurrentBoughtRepository;
        this.playerService = playerService;
        this.scoresService = scoresService;
        this.priceStrategy = priceStrategy;
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

    public List<PlayerUserCurrentBoughtDto> getUserBoughtPlayers(Long userId) throws InterruptedException {
        logger.info("getting user id {} bought players", userId);

        List<PlayerUserCurrentBought> userCurrBoughtPlayers = playerUserCurrentBoughtRepository.getUserCurrBoughtPlayers(userId);
        List<PlayerUserCurrentBoughtDto> userPlayersList = new ArrayList<>();

        for (PlayerUserCurrentBought player : userCurrBoughtPlayers) {
            Player playerEntity = player.getPlayer();
            PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(playerEntity);
            // world cup mode prices on tournament stats, not club league stats, so skip the external
            // club refresh entirely — it's unnecessary AND would NPE for WC-seeded players who have
            // no league_stats row (mirrors UserService.getAllUsersScores).
            boolean needsRefresh = !playerDetailsResDto.getAreClubStatsUpdated()
                    || (playerEntity.getUpdatedBy() != null && playerEntity.getUpdatedBy().contains("sofascore"));
            if (!priceStrategy.isWorldCupMode() && needsRefresh) {
                PlayerDetailsDto playerDetailsDto = scoresService.fetchPlayerEntityFromExternalService(playerEntity.getSofascoreId());
                // guard: external service may be down or have no league stats for this player
                if (playerDetailsDto != null && playerDetailsDto.getLeagueStats() != null) {
                    PlayerLeagueStats existing = playerEntity.getLeagueStats();
                    if (existing != null) {
                        // reuse the managed entity (avoids "different object with same id" in the session)
                        existing.setRating(playerDetailsDto.getLeagueStats().getRating());
                        existing.setGoals(playerDetailsDto.getLeagueStats().getGoals());
                        existing.setAssists(playerDetailsDto.getLeagueStats().getAssists());
                        existing.setTotalNumOfGames(playerDetailsDto.getLeagueStats().getAppearances());
                        playerService.updatePlayerLeagueStatsDetails(playerEntity.getId(), existing);
                    } else {
                        // no league row yet (e.g. a player who never had one): insert a fresh one
                        playerService.updatePlayerLeagueStatsDetails(playerEntity.getId(),
                                scoresService.createLeagueStatsDto(playerDetailsDto.getLeagueStats()));
                    }
                }
            }
            playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(playerEntity);
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
