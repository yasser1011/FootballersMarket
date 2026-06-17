package com.yasser.footballersmarket.worldcup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface WorldCupFixtureRepository extends JpaRepository<WorldCupFixture, Long> {

    // results-poll candidates: kicked off (date < now) and not in a finished-or-dead status.
    // pass EXCLUDED_FROM_RESULTS_POLL as statuses.
    List<WorldCupFixture> findByStatusNotInAndDateBefore(Collection<String> statuses, Instant date);

    // fixtures whose kickoff falls in [start, end); used by the today+tomorrow fixtures view
    List<WorldCupFixture> findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(Instant start, Instant end);

    // a team's fixtures that have kicked off (date in the past) and aren't finished yet.
    // the caller decides "still live" by checking now against kickoff + checkAgainMinutes.
    @Query("select f from WorldCupFixture f where (f.homeTeamId = ?1 or f.awayTeamId = ?1) "
            + "and f.status not in ?2 and f.date <= ?3")
    List<WorldCupFixture> findKickedOffNotFinishedForTeam(Long teamId, Collection<String> finishedStatuses, Instant now);
}
