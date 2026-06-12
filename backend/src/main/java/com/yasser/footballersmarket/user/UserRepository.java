package com.yasser.footballersmarket.user;

import com.yasser.footballersmarket.player.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("update User u set u.points = ?2 where u.id = ?1")
    void updateUserPoints(Long userId, Integer points);


    String topRankedUsersQuery = "select x.user_id, max(x.username) as username, coalesce(sum(x.price), 0) + max(x.points) as score from (\n" +
            "\tselect ps.player_id, ps.user_id, ps.username, ps.points,\n" +
            "\tfn_get_price(cast(FLOOR((CURRENT_DATE - date (ps.date_of_birth)) / 365.25) as integer), \n" +
            "\tcast(ps.avg_rating as numeric), ps.total_goals, ps.total_assists) as price\n" +
            "\tfrom  (\n" +
            "\t\tselect pu.player_id, u.id as user_id, u.username, p.date_of_birth, u.points,\n" +
            "\t\tCOALESCE(pcl.goals, 0) + COALESCE(pl.goals, 0) as total_goals,\n" +
            "\t\tCOALESCE(pcl.assists, 0) + COALESCE(pl.assists, 0) as total_assists,\n" +
            "\t\tcase when pcl.rating is null and pl.rating is null then 0\n" +
            "\t\twhen pcl.rating is null then pl.rating\n" +
            "\t\twhen pl.rating is null then pcl.rating\n" +
            "\t\telse cast((pl.rating + pcl.rating) as numeric) / 2\n" +
            "\t\tend as avg_rating \n" +
            "\t\t\tfrom usr u \n" +
            "\t\t\tleft join player_user_current_bought pu on u.id = pu.user_id\n" +
            "\t\t\tleft join player p on pu.player_id = p.id\n" +
            "\t\t\tleft join player_champions_league_stats pcl on p.id = pcl.player_id\n" +
            "\t\t\tleft join player_league_stats pl on pl.player_id = p.id\n" +
            "\n" +
            "\t) ps\n" +
            ") x group by x.user_id\n" +
            "\n" +
            "order by sum(x.price) desc\n" +
            "limit 5";
    @Query(nativeQuery = true, value = topRankedUsersQuery)
    List<Object[]> getTopRankedUsers();

    String usersScoreQuery = "select ps.player_id, ps.user_id, ps.username, ps.points, ps.sofascore_id, ps.updated_by,\n" +
            "\tps.sofascore_club, ps.rapid_api_club,\n" +
            "\tfn_get_price(cast(FLOOR((CURRENT_DATE - date (ps.date_of_birth)) / 365.25) as integer), \n" +
            "\tcast(ps.avg_rating as numeric), ps.total_goals, ps.total_assists) as price\n" +
            "\tfrom  (\n" +
            "\t\tselect pu.player_id, u.id as user_id, u.username, p.sofascore_club, p.rapid_api_club,\n" +
            "\t\tp.date_of_birth, p.sofascore_id, p.updated_by, u.points,\n" +
            "\t\tCOALESCE(pcl.goals, 0) + COALESCE(pl.goals, 0) as total_goals,\n" +
            "\t\tCOALESCE(pcl.assists, 0) + COALESCE(pl.assists, 0) as total_assists,\n" +
            "\t\tcase when pcl.rating is null and pl.rating is null then 0\n" +
            "\t\twhen pcl.rating is null then pl.rating\n" +
            "\t\twhen pl.rating is null then pcl.rating\n" +
            "\t\telse cast((pl.rating + pcl.rating) as numeric) / 2\n" +
            "\t\tend as avg_rating \n" +
            "\t\t\tfrom usr u \n" +
            "\t\t\tleft join player_user_current_bought pu on u.id = pu.user_id\n" +
            "\t\t\tleft join player p on pu.player_id = p.id\n" +
            "\t\t\tleft join player_champions_league_stats pcl on p.id = pcl.player_id\n" +
            "\t\t\tleft join player_league_stats pl on pl.player_id = p.id\n" +
            "\n" +
            "\t) ps";
    @Query(nativeQuery = true, value = usersScoreQuery)
    List<Object[]> getAllUsersScore();

    // world cup mode variants: portfolio valued by fn_get_wc_price.
    // participants use their frozen base_rating + live tournament rating;
    // non participants fall back to their league rating (flat base price).
    // the case guard keeps users without any bought player at null price (not a 250 phantom)
    String wcTopRankedUsersQuery = "select x.user_id, max(x.username) as username,\n" +
            "\tcoalesce(sum(x.price), 0) + max(x.points) as score from (\n" +
            "\tselect u.id as user_id, u.username, u.points,\n" +
            "\tcase when pu.player_id is null then null\n" +
            "\telse fn_get_wc_price(cast(coalesce(wcs.base_rating, pl.rating) as numeric), cast(wcs.rating as numeric)) end as price\n" +
            "\t\tfrom usr u\n" +
            "\t\tleft join player_user_current_bought pu on u.id = pu.user_id\n" +
            "\t\tleft join player p on pu.player_id = p.id\n" +
            "\t\tleft join player_world_cup_stats wcs on wcs.player_id = p.id\n" +
            "\t\tleft join player_league_stats pl on pl.player_id = p.id\n" +
            ") x group by x.user_id\n" +
            "order by sum(x.price) desc\n" +
            "limit 5";
    @Query(nativeQuery = true, value = wcTopRankedUsersQuery)
    List<Object[]> getTopRankedUsersWorldCup();

    String wcUsersScoreQuery = "select pu.player_id, u.id as user_id, u.username, u.points,\n" +
            "\tp.sofascore_id, p.updated_by, p.sofascore_club, p.rapid_api_club,\n" +
            "\tcase when pu.player_id is null then null\n" +
            "\telse fn_get_wc_price(cast(coalesce(wcs.base_rating, pl.rating) as numeric), cast(wcs.rating as numeric)) end as price\n" +
            "\t\tfrom usr u\n" +
            "\t\tleft join player_user_current_bought pu on u.id = pu.user_id\n" +
            "\t\tleft join player p on pu.player_id = p.id\n" +
            "\t\tleft join player_world_cup_stats wcs on wcs.player_id = p.id\n" +
            "\t\tleft join player_league_stats pl on pl.player_id = p.id";
    @Query(nativeQuery = true, value = wcUsersScoreQuery)
    List<Object[]> getAllUsersScoreWorldCup();

    Optional<User> findById(Long id);
}
