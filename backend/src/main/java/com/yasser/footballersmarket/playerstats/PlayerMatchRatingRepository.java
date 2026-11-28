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

    // the featured players: the 5 best ratings of the round each competition is on -- the round of
    // its most recent fixture with stats applied, so it switches the moment the new round's first
    // match is applied. ties: more minutes first.
    //  - while the CL's latest applied match is newer than every league's: the CL round alone
    //  - otherwise: the leagues' current rounds together, no CL
    // which also keeps trimToLatest5 harmless: the CL counts only while no league match came after
    // it, so its rows are each player's newest; a league's current round is that league's newest,
    // with at most a CL match or two after it. no age limit: a league between rounds keeps showing
    // its last one. at season rollover a same-named round of the previous season would match too
    // (see the season rollover item)
    // rows: [0] player id, [1] rating, [2] minutes, [3] home team id, [4] away team id
    @Query(value = """
            with current_round as (
                select distinct on (f.league_id) f.league_id, f.round, f.date as latest
                from fixture f
                where f.stats_applied = true
                order by f.league_id, f.date desc
            ),
            champions_league_is_newest as (
                select coalesce(
                    (select latest from current_round where league_id = :championsLeagueId)
                        > coalesce((select max(latest) from current_round where league_id <> :championsLeagueId),
                                   '-infinity'),
                    false) as newest
            )
            select r.player_id, r.rating, r.minutes, f.home_team_id, f.away_team_id
            from player_match_rating r
            join fixture f        on f.id = r.fixture_id
            join current_round cr on cr.league_id = f.league_id and cr.round = f.round
            where r.rating is not null
              and (cr.league_id = :championsLeagueId) = (select newest from champions_league_is_newest)
            order by r.rating desc, r.minutes desc nulls last
            limit 5
            """, nativeQuery = true)
    List<Object[]> findTop5OfCurrentRounds(@Param("championsLeagueId") Long championsLeagueId);
}
