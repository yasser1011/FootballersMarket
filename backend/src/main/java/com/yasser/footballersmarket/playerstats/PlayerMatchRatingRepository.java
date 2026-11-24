package com.yasser.footballersmarket.playerstats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PlayerMatchRatingRepository extends JpaRepository<PlayerMatchRating, PlayerMatchRating.Key> {

    List<PlayerMatchRating> findTop5ByPlayerIdOrderByFixtureDateDesc(Long playerId);

    // keeps each given player's 5 newest rows by fixture date and deletes the rest. by date, not by
    // insert order: a late result for an older fixture lands 6th and is dropped straight away
    @Modifying
    @Query(value = """
            delete from player_match_rating p
            using (select player_id, fixture_id,
                          row_number() over (partition by player_id order by fixture_date desc) rn
                   from player_match_rating
                   where player_id in (:playerIds)) ranked
            where p.player_id = ranked.player_id
              and p.fixture_id = ranked.fixture_id
              and ranked.rn > 5
            """, nativeQuery = true)
    void trimToLatest5(@Param("playerIds") Collection<Long> playerIds);
}
