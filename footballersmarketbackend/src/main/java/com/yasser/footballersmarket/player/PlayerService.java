package com.yasser.footballersmarket.player;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.sofascore.dto.PlayerLeagueStatsDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@Transactional
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final ScoresService scoresService;
    private final PlayerLeagueStatsService playerLeagueStatsService;
    Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public PlayerService(PlayerRepository playerRepository,
                         PlayerLeagueStatsService playerLeagueStatsService, ScoresService scoresService) {
        this.playerRepository = playerRepository;
        this.playerLeagueStatsService = playerLeagueStatsService;
        this.scoresService = scoresService;
    }

    public PlayerDetailsResDto getPlayerDetailsById(Long rapidApiId, Integer sofascoreId){
        // todo
        // check stats updated flag if not get stats from sofascore service and update db stats and add updated by sofascore

        // 1- check if both ids available -> means he was already checked from before and his stats not updated
        // get stats from sofascore service and update db data
        // will need to fetch his details again anyway to get CL stats
        // 2- check if rapid id available
        // get player from db and check status flag to update his local data
        // 3- check if sofascore id available
        // get player from sofascore and check status flag to update his local data
        logger.info("getting player details player id {} sofascore id {}", rapidApiId, sofascoreId);
        try {
            PlayerDetailsResDto playerDto = null;
            // if both available just return stats from sofascore and update db stats
            if (rapidApiId != 0 && sofascoreId != 0){
                playerDto = getPlayerDbDetails(rapidApiId);
                return updatePlayerStatsInDbAndReturnUpdatedStatsPlayerResponse(playerDto);
            }
            else if (rapidApiId != 0){
                logger.info("getting player from database player id {}", rapidApiId);
                playerDto = getPlayerDbDetails(rapidApiId);
            }else if(sofascoreId != 0){
                logger.info("getting player from db by sofascore id {}", sofascoreId);
                playerDto = getPlayerBySofascoreId(sofascoreId);
            }
            if (playerDto == null){
                logger.error("error retrieving player details player id {} sofascore id {}", rapidApiId, sofascoreId);
                return null;
            }

            if (!playerDto.getAreClubStatsUpdated()){
                return updatePlayerStatsInDbAndReturnUpdatedStatsPlayerResponse(playerDto);
            }
            logger.info("retrieved player details from database sofascore id {}, player id {}", sofascoreId, rapidApiId);
            return playerDto;
        }catch(Exception e){
            logger.error("error getting player details player id {} , sofascore id {}", rapidApiId, sofascoreId);
            throw new IllegalStateException("couldn't get player details player id "
                    + rapidApiId + " , sofascore id " + sofascoreId + " error " + e.getMessage());
        }
    }

    private PlayerLeagueStatsDetails createLeagueStatsDetails(PlayerLeagueStats leagueStats) {
        Double rating = leagueStats.getRating();
        Integer goals = leagueStats.getGoals();
        Integer total = goals + leagueStats.getAssists();
        Integer totalNumOfGames = leagueStats.getTotalNumOfGames();
        return new PlayerLeagueStatsDetails(rating, goals, total, totalNumOfGames);
    }

    private PlayerDetailsResDto updatePlayerStatsInDbAndReturnUpdatedStatsPlayerResponse(PlayerDetailsResDto playerDto) {
        Integer sofascoreId = playerDto.getExternalServicePlayerId();
        logger.info("Player details not updated in database, retrieving from external service by id {}", sofascoreId);

        PlayerDetailsResDto externalPlayer = scoresService.getPlayerEntityFromExternalService(sofascoreId);
        if (externalPlayer == null) {
            logger.error("Couldn't get player details from external service, sofascoreId: {}", sofascoreId);
            throw new IllegalStateException("Player not found in external service");
        }
        // Update league stats
        PlayerLeagueStats leagueStats = externalPlayer.getLeagueStats();
        // if no data early return and don't update db
        if(leagueStats == null){
            playerDto.setRecentMatches(externalPlayer.getRecentMatches());
            return playerDto;
        }

        playerDto.setLeagueStats(leagueStats);
        playerDto.setRecentMatches(externalPlayer.getRecentMatches());

        // Prepare and update stats details
        PlayerLeagueStatsDetails statsDetails = createLeagueStatsDetails(leagueStats);
        updatePlayerLeagueStatsDetails(playerDto.getId(), statsDetails);

        logger.info("Updated player stats from external service - sofascoreId: {}", sofascoreId);
        return playerDto;
    }


    public List<PlayerBasic> getPlayersUpdatedBySofascore(){
        logger.info("retrieving players which are updated by sofascore");
        return playerRepository.getPlayersUpdatedBySofascore();
    }
    public List<Integer> getPlayersWithoutRapidId(){
        logger.info("retrieving players which was bought from sofascore search");
        return playerRepository.getPlayersWithoutRapidId();
    }

    private PlayerDetailsResDto getPlayerDbDetails(Long id) throws JsonProcessingException {
        // 1- check if the player has updated by sofascore flag
        // 2- check his name in sofascore to see if his club name is updated
        // 3- decide if data should be returned from db or from sofascore

         Player playerLocalDb = playerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id " + id));
         logger.info("player found in database, player id {}", id);
        return convertPlayerEntityToPlayerDto(playerLocalDb);

    }

    private PlayerLeagueStats updatePlayerLeagueStatsDetails(Long rapidId, PlayerLeagueStatsDetails playerSofascoreLeagueStats){
        logger.info("updating player data in database player id {}", rapidId);
        Integer goals = playerSofascoreLeagueStats.getGoals();
        Integer assists = playerSofascoreLeagueStats.getGoalsAssistsSum() - goals;
        Integer numOfGames = playerSofascoreLeagueStats.getCountRating();
        Double rating = playerSofascoreLeagueStats.getRating();
        PlayerLeagueStats playerLeagueStats = new PlayerLeagueStats(rapidId, goals,
                assists, rating, numOfGames);
        playerLeagueStatsService.savePlayerLeagueStats(playerLeagueStats);
        updatePlayerUpdatedByStatusToSofascore(rapidId);

        logger.info("player data updated in database player id {}", rapidId);
        return playerLeagueStats;
    }

    public void deleteRapidDataPlayers(){
        logger.info("deleting players data");
        playerRepository.deleteRapidDataPlayers();
    }
    public void deletePlayerBySofascoreId(Integer playerSofascoreId){
        logger.info("deleting player by sofascore id, {}", playerSofascoreId);
        playerRepository.deletePlayer(playerSofascoreId);
    }

    public void deleteAllPlayersBySofascoreIds(List<Integer> playerSofascoreIds){
        logger.info("deleting all players by sofascore ids");
        playerRepository.deleteAllPlayersBySofascoreIds(playerSofascoreIds);
    }

    public List<Long> getRapidDataPlayerIds(){
        return playerRepository.getRapidDataPlayerIds();
    }

    public List<PlayerExternalServiceBasicDetails> getPlayerExternalServiceBasicDetails(){
        return playerRepository.getPlayerExternalServiceBasicDetails();
    }

    public void savePlayersList(List<Player> playerEntityList){
        playerRepository.saveAll(playerEntityList);
    }

    public void savePlayer(Player playerEntity){
        playerRepository.save(playerEntity);
    }

    public Page<PlayerDetailsResDto> getHomePagePlayerData(int pageNum){
        // page num starts with 0
        Pageable pageData = PageRequest.of(pageNum, 50, Sort.by("ls.rating").descending().and(Sort.by("id").ascending()));
        Page<Player> homePagePlayers = playerRepository.getHomePagePlayers(pageData);
        Page<PlayerDetailsResDto> map = homePagePlayers.map(this::convertPlayerEntityToPlayerDto);
        return map;
    }

    public void updatePlayerUpdatedByStatusToSofascore(Long playerId){
        logger.info("updating player updated by flag to sofascore, player id {}", playerId);
        playerRepository.updatePlayerUpdatedByStatusToSofascore(playerId);
    }

    public PlayerDetailsResDto getPlayerBySofascoreId(Integer sofascoreId){
        Player oneBySofascoreId = playerRepository.findFirstBySofascoreId(sofascoreId);
        return convertPlayerEntityToPlayerDto(oneBySofascoreId);
    }

    public PlayerDetailsResDto getPlayerByIdAndSofascoreId(Long playerId, Integer sofascoreId){
        Player oneByIdAndSofascoreId = playerRepository.findOneByIdAndSofascoreId(playerId, sofascoreId);
        return convertPlayerEntityToPlayerDto(oneByIdAndSofascoreId);
    }

    public void deleteAll(){
        playerRepository.deleteAll();
    }

    public PlayerDetailsResDto convertPlayerEntityToPlayerDto(Player playerEntity){
        if (playerEntity == null) return null;
        PlayerDetailsResDto playerDto = new PlayerDetailsResDto();
        playerDto.setId(playerEntity.getId());
        playerDto.setExternalServicePlayerId(playerEntity.getSofascoreId());
        playerDto.setName(playerEntity.getName());
        playerDto.setNationality(playerEntity.getNationality());
        playerDto.setDateOfBirth(playerEntity.getDateOfBirth());
        playerDto.setPosition(playerEntity.getPosition());
        playerDto.setRapidApiClub(playerEntity.getRapidApiClub());
        playerDto.setExternalServicePlayerClub(playerEntity.getSofascoreClub());
        playerDto.setCurrentlyInjured(playerEntity.getCurrentlyInjured());
        playerDto.setLeagueStats(playerEntity.getLeagueStats());
        playerDto.setPhotoUrl(playerEntity.getPhotoUrl());
        playerDto.setClubPhotoUrl(playerEntity.getClubPhotoUrl());
        playerDto.setUpdatedBy(playerEntity.getUpdatedBy());

        return playerDto;
    }

    public Player convertPlayerDtoToPlayerEntity(PlayerDetailsResDto playerDto){
        if (playerDto == null) return null;
        Player player = new Player();
        player.setId(playerDto.getId());
        player.setSofascoreId(playerDto.getExternalServicePlayerId());
        player.setName(playerDto.getName());
        player.setNationality(playerDto.getNationality());
        player.setDateOfBirth(playerDto.getDateOfBirth());
        player.setPosition(playerDto.getPosition());
        player.setRapidApiClub(playerDto.getRapidApiClub());
        player.setSofascoreClub(playerDto.getExternalServicePlayerClub());
        player.setCurrentlyInjured(playerDto.getCurrentlyInjured());
        player.setLeagueStats(playerDto.getLeagueStats());
        player.setPhotoUrl(playerDto.getPhotoUrl());
        player.setClubPhotoUrl(playerDto.getClubPhotoUrl());
        player.setUpdatedBy(playerDto.getUpdatedBy());

        return player;
    }

}
