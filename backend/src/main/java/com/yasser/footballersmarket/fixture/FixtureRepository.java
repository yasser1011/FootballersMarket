package com.yasser.footballersmarket.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    // for the public fixtures view: everything kicking off inside a window, soonest first
    List<Fixture> findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(Instant start, Instant end);

    // results poll candidates: not yet finished-or-dead, and kicked off long enough ago that the
    // match should be over but recently enough to still be worth a request.
    // a null status counts as pollable -- `status not in (...)` would evaluate to null and silently
    // drop exactly the rows whose state we do not know
    @Query("select f from Fixture f "
            + "where (f.status is null or f.status not in :excludedStatuses) "
            + "and f.date <= :latestKickoff and f.date >= :earliestKickoff "
            + "order by f.date asc")
    List<Fixture> findPollable(@Param("excludedStatuses") Set<String> excludedStatuses,
                               @Param("earliestKickoff") Instant earliestKickoff,
                               @Param("latestKickoff") Instant latestKickoff);
}
