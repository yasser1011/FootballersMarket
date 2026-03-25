package com.yasser.footballersmarket.playerstats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerChampionsLeagueStatsRepository extends JpaRepository<PlayerChampionsLeagueStats, Long> {
    @Modifying
    @Query("delete from PlayerChampionsLeagueStats pcl where pcl.player in (select p from Player p where p.sofascoreId = ?1)")
    void deletePlayerClStatsBySofascoreId(Integer sofascoreId);

    @Modifying
    @Query("delete from PlayerChampionsLeagueStats pcl where pcl.player in (select p from Player p where p.sofascoreId in ?1)")
    void deleteAllPlayersClStatsBySofascoreIds(List<Integer> sofascoreIds);
}
