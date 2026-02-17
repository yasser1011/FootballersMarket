package com.yasser.footballersmarket.player;

import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.player.dto.PlayerHomePageDto;
import com.yasser.footballersmarket.player.dto.PlayerResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
//    List<Player> findByNameContainingIgnoreCaseAndClubContainingIgnoreCase(String nameSubstring, String clubSubstring);
    List<Player> findByNameContainingIgnoreCase(String fullPlayerName);

//    @Query("SELECT p FROM player b join b.category c where c.category = :category")
//    @Query("SELECT p.id, p.sofascoreId, p.name, p.club, p.updatedBy FROM Player p where p.updatedBy = 'sofascore'") returns null fields
    @Query("select new com.yasser.footballersmarket.player.Player(p.id, p.sofascoreId, p.name, p.rapidApiClub, p.updatedBy) from Player p where p.updatedBy = 'sofascore'")
    List<PlayerBasic> getPlayersUpdatedBySofascore();

    @Query("select p.sofascoreId from Player p where p.updatedBy = 'sofascore_buy'")
    List<Integer> getPlayersWithoutRapidId();

    Optional<Player> findById(Long id);

    @Modifying
    @Query("delete from Player p where p.updatedBy <> 'sofascore' or p.updatedBy is null")
    void deleteRapidDataPlayers();

    @Modifying
    @Query("delete from Player p where p.sofascoreId = ?1")
    void deletePlayer(Integer playerSofascoreId);

    @Modifying
    @Query("delete from Player p where p.sofascoreId in ?1")
    void deleteAllPlayersBySofascoreIds(List<Integer> playerSofascoreIds);

    @Query("select p.id from Player p where p.updatedBy <> 'sofascore' or p.updatedBy is null")
    List<Long> getRapidDataPlayerIds();

    @Query("select new com.yasser.footballersmarket.player.Player(p.id, p.sofascoreId, p.rapidApiClub, p.sofascoreClub, p.photoUrl, p.clubPhotoUrl) from Player p where p.updatedBy <> 'sofascore' or p.updatedBy is null")
    List<PlayerExternalServiceBasicDetails> getPlayerExternalServiceBasicDetails();

    Player findPlayerById(Long id);

    @Query("SELECT p FROM Player p join p.leagueStats ls")
    Page<Player> getHomePagePlayers(Pageable pageable);

    @Modifying
    @Query("UPDATE Player p SET p.updatedBy = 'sofascore' where p.id = ?1")
    void updatePlayerUpdatedByStatusToSofascore(Long playerId);

    Player findFirstBySofascoreId(Integer sofascoreId);

    Player findOneByIdAndSofascoreId(Long playerId, Integer sofascoreId);

    String playersDtoQuery = "\n" +
            "with ps as (\n" +
            "\t\n" +
            "select p.id, p.sofascore_id as sofascoreId, p.name, p.nationality, p.date_of_birth as dateOfBirth,\n" +
            "\tp.position, p.rapid_api_club as rapidApiClub,\n" +
            "\tp.sofascore_club as sofascoreClub, p.currently_injured as currentlyInjured,\n" +
            "\tpl.player_id as leaguePlayerId, \n" +
            "\tpl.total_num_of_games as leagueNumOfGames, pl.goals as leagueGoals, \n" +
            "\tpl.assists as leagueAssists, pl.rating as leagueRating,\n" +
            "\tpcl.player_id as clPlayerId,\n" +
            "\tpcl.total_num_of_games as clNumOfGames, pcl.goals as clGoals,\n" +
            "\tpcl.assists as clAssists, pcl.rating as clRating,\n" +
            "\tp.photo_url as photoUrl, p.club_photo_url as clubPhotoUrl, p.updated_by as updatedBy,\n" +
            "COALESCE(pcl.goals, 0) + COALESCE(pl.goals, 0) as total_goals,\n" +
            "COALESCE(pcl.assists, 0) + COALESCE(pl.assists, 0) as total_assists,\n" +
            "case when pcl.rating is null and pl.rating is null then 0\n" +
            "when pcl.rating is null then pl.rating\n" +
            "when pl.rating is null then pcl.rating\n" +
            "else cast((pl.rating + pcl.rating) as numeric) / 2\n" +
            "end as avg_rating \n" +
            "\tfrom player p \n" +
            "left join player_champions_league_stats pcl on p.id = pcl.player_id\n" +
            "left join player_league_stats pl on pl.player_id = p.id\n" +
            "\n" +
            ")\n" +
            "select id, sofascoreId, name, nationality, dateOfBirth, position, rapidApiClub, sofascoreClub,\n" +
            "currentlyInjured, leaguePlayerId, leagueNumOfGames, leagueGoals, leagueAssists, leagueRating,\n" +
            "clPlayerId, clNumOfGames, clGoals, clAssists, clRating, \n" +
            "--round(cast(avg_rating as numeric), 2) as rating, \n" +
            "fn_get_price(cast(FLOOR((CURRENT_DATE - date (ps.dateOfBirth)) / 365.25) as integer), \n" +
            "cast(ps.avg_rating as numeric), ps.total_goals, ps.total_assists) as price,\n" +
            "photoUrl, clubPhotoUrl, updatedBy\n" +
            " from ps\n";

    @Query(nativeQuery = true, value = playersDtoQuery)
    List<Object[]> getPlayersDtoList();
}
