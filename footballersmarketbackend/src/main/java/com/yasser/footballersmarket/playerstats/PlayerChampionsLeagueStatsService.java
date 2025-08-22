package com.yasser.footballersmarket.playerstats;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlayerChampionsLeagueStatsService {
    private final PlayerChampionsLeagueStatsRepository playerChampionsLeagueStatsRepository;

    public PlayerChampionsLeagueStatsService(PlayerChampionsLeagueStatsRepository playerChampionsLeagueStatsRepository){
        this.playerChampionsLeagueStatsRepository = playerChampionsLeagueStatsRepository;
    }

    public void deletePlayerClStatsBySofascoreId(Integer playerSofascoreId){
        playerChampionsLeagueStatsRepository.deletePlayerClStatsBySofascoreId(playerSofascoreId);
    }

    public void deleteAllPlayersClStatsBySofascoreIds(List<Integer> playerSofascoreIds){
        playerChampionsLeagueStatsRepository.deleteAllPlayersClStatsBySofascoreIds(playerSofascoreIds);
    }

}
