package com.yasser.footballersmarket.player;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.pricing.PlayerPricingData;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsService;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
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
    private final PlayerLeagueStatsService playerLeagueStatsService;
    private final PriceStrategy priceStrategy;
    private final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public PlayerService(PlayerRepository playerRepository,
                         PlayerLeagueStatsService playerLeagueStatsService,
                         PriceStrategy priceStrategy) {
        this.playerRepository = playerRepository;
        this.playerLeagueStatsService = playerLeagueStatsService;
        this.priceStrategy = priceStrategy;
    }

    public PlayerDetailsResDto getPlayerDetailsByInternalId(Long rapidApiId) {
        Player playerLocalDb = playerRepository.findById(rapidApiId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found with id " + rapidApiId));
        logger.info("player found in database, player id {}", rapidApiId);
        return convertPlayerEntityToPlayerDto(playerLocalDb);
    }

    public List<PlayerBasic> getPlayersUpdatedBySofascore(){
        logger.info("retrieving players which are updated by sofascore");
        return playerRepository.getPlayersUpdatedBySofascore();
    }
    public List<Integer> getPlayersWithoutRapidId(){
        logger.info("retrieving players which was bought from sofascore search");
        return playerRepository.getPlayersWithoutRapidId();
    }

    public void updatePlayerLeagueStatsDetails(Long rapidId, PlayerLeagueStats updatedLeagueStats){
        logger.info("updating player data in database player id {}", rapidId);
        // PlayerLeagueStats derives its @Id from the @MapsId player relationship. existing row ->
        // set the id so save() merges/updates it. brand-new row (e.g. a world-cup-seeded player who
        // never had league stats) -> leave the id null and set the player so save() persists and
        // @MapsId fills the id from it; setting the id here would make save() merge and fail to
        // derive the identifier ("null identifier").
        if (playerLeagueStatsService.leagueStatsExist(rapidId)) {
            updatedLeagueStats.setPlayerId(rapidId);
        } else {
            updatedLeagueStats.setPlayer(playerRepository.getReferenceById(rapidId));
        }
        playerLeagueStatsService.savePlayerLeagueStats(updatedLeagueStats);
        updatePlayerUpdatedByStatusToSofascore(rapidId);
        logger.info("player data updated in database player id {}", rapidId);
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

    public Page<PlayerDetailsResDto> getHomePagePlayerData(int pageNum, boolean worldCup, Long teamId){
        // page num starts with 0
        if (worldCup) {
            // world cup page: sort by tournament rating; teamId optionally filters by national team
            Pageable wcPageData = PageRequest.of(pageNum, 50, Sort.by("wcs.rating").descending().and(Sort.by("id").ascending()));
            return playerRepository.getWorldCupHomePagePlayers(teamId, wcPageData)
                    .map(this::convertPlayerEntityToPlayerDto);
        }
        Pageable pageData = PageRequest.of(pageNum, 50, Sort.by("ls.rating").descending().and(Sort.by("id").ascending()));
        Page<Player> homePagePlayers = playerRepository.getHomePagePlayers(pageData);
        Page<PlayerDetailsResDto> map = homePagePlayers.map(this::convertPlayerEntityToPlayerDto);
        return map;
    }

    public void updatePlayerUpdatedByStatusToSofascore(Long playerId){
        logger.info("updating player updated by flag to sofascore, player id {}", playerId);
        playerRepository.updatePlayerUpdatedByStatusToSofascore(playerId);
    }

    public PlayerDetailsResDto getPlayerDetailsByExternalId(Integer sofascoreId){
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
        PlayerWorldCupStats wcStats = playerEntity.getWorldCupStats();
        playerDto.setWorldCupBaseRating(wcStats == null ? null : wcStats.getBaseRating());
        playerDto.setWorldCupRating(wcStats == null ? null : wcStats.getRating());
        playerDto.setWorldCupGames(wcStats == null ? null : wcStats.getTotalNumOfGames());
        // exposed tournament stats (incl. the national team via wcStats.team for the flag + name)
        playerDto.setWorldCupStats(wcStats);
        priceDto(playerDto);

        return playerDto;
    }

    // single pricing entry point: reads the dto's current state (incl. world cup inputs)
    // and sets its price via the active strategy. call again after mutating a dto's stats
    public void priceDto(PlayerDetailsResDto dto) {
        dto.setPrice(priceStrategy.calculatePrice(new PlayerPricingData(
                dto.getDateOfBirth(), dto.getAvgRating(),
                dto.getWorldCupBaseRating(), dto.getWorldCupRating(), dto.getWorldCupGames())));
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
