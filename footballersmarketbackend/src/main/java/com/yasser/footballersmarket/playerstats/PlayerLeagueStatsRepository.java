package com.yasser.footballersmarket.playerstats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerLeagueStatsRepository extends JpaRepository<PlayerLeagueStats, Long> {
    @Modifying
    @Query("delete from PlayerLeagueStats pl where pl.player in (select p from Player p where (p.updatedBy <> 'sofascore' or p.updatedBy is null) and p.id in ?1)")
    void deletePlayersLeagueStatsRapidData(List<Long> ids);

    @Query("select pl.playerId from PlayerLeagueStats pl where pl.player in (select p from Player p where p.updatedBy <> 'sofascore' or p.updatedBy is null)")
    List<Long> getRapidDataPlayerLeagueStatsIds();

    @Modifying
    @Query("delete from PlayerLeagueStats pl where pl.player in (select p from Player p where p.sofascoreId = ?1)")
    void deletePlayerLeagueStatsBySofascoreId(Integer playerSofascoreId);

    @Modifying
    @Query("delete from PlayerLeagueStats pl where pl.player in (select p from Player p where p.sofascoreId in ?1)")
    void deleteAllPlayersLeagueStatsBySofascoreIds(List<Integer> playerSofascoreIds);
}
