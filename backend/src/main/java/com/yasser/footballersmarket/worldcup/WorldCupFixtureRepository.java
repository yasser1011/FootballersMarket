package com.yasser.footballersmarket.worldcup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface WorldCupFixtureRepository extends JpaRepository<WorldCupFixture, Long> {

    // poll candidates: kicked off (date in the past) and not yet in a finished state.
    // the per-fixture checkAgainMinutes backoff window is applied in code by the scheduler.
    List<WorldCupFixture> findByStatusNotInAndDateBefore(Collection<String> statuses, Instant date);

    // fixtures whose kickoff falls in [start, end); used by the today+tomorrow fixtures view
    List<WorldCupFixture> findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(Instant start, Instant end);
}
