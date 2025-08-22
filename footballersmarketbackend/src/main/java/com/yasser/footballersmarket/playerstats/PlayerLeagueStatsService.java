package com.yasser.footballersmarket.playerstats;

import com.yasser.footballersmarket.player.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlayerLeagueStatsService {
    private final PlayerLeagueStatsRepository playerLeagueStatsRepository;
    Logger logger = LoggerFactory.getLogger(PlayerLeagueStatsService.class);

    public PlayerLeagueStatsService(PlayerLeagueStatsRepository playerLeagueStatsRepository){
        this.playerLeagueStatsRepository = playerLeagueStatsRepository;
    }

    public void deletePlayersLeagueStatsRapidData(List<Long> ids){
        logger.info("deleting players stats");
        playerLeagueStatsRepository.deletePlayersLeagueStatsRapidData(ids);
    }

    public void deletePlayerLeagueStatsBySofascoreId(Integer playerSofascoreId){
        logger.info("deleting player stats by sofascore id {}", playerSofascoreId);
        playerLeagueStatsRepository.deletePlayerLeagueStatsBySofascoreId(playerSofascoreId);
    }

    public void deleteAllPlayersLeagueStatsBySofascoreId(List<Integer> playerSofascoreIds){
        logger.info("deleting players stats by sofascore ids list");
        playerLeagueStatsRepository.deleteAllPlayersLeagueStatsBySofascoreIds(playerSofascoreIds);
    }

    public void savePlayersLeagueStatsRapidData(List<PlayerLeagueStats> playerLeagueStatsList){
        logger.info("saving players league stats");
        playerLeagueStatsRepository.saveAll(playerLeagueStatsList);
    }

    public void savePlayerLeagueStats(PlayerLeagueStats playerLeagueStats){
        logger.info("saving player league stats");
        playerLeagueStatsRepository.save(playerLeagueStats);
    }

    public List<Long> getRapidDataPlayerLeagueStatsIds(){
        return playerLeagueStatsRepository.getRapidDataPlayerLeagueStatsIds();
    }
}
