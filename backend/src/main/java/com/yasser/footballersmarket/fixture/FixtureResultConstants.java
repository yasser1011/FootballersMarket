package com.yasser.footballersmarket.fixture;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public final class FixtureResultConstants {
    private FixtureResultConstants() {}

    // --- api status short codes ---
    // the match is over -> its result is final
    public static final Set<String> FINISHED_STATUSES = Set.of("FT", "AET", "PEN");
    // the match will never produce a normal result (postponed/abandoned/technical) -> stop polling
    public static final Set<String> DEAD_STATUSES = Set.of("CANC", "ABD", "SUSP", "TBD", "AWD", "WO");
    // finished-or-dead: skipped by the poll. everything else (in-play 1H/HT/2H/ET/BT/P and
    // not-started NS) stays in the poll until it reaches one of these
    public static final Set<String> EXCLUDED_FROM_RESULTS_POLL = buildExcluded();

    // results are polled per fixture, once the match should already be over, because there is no
    // live view to serve -- that keeps it to roughly one request per fixture instead of repeated
    // polling through the match.
    // earliest: 90 mins + half time + stoppage, with a margin
    public static final Duration EARLIEST_CHECK_AFTER_KICKOFF = Duration.ofMinutes(125);
    // latest: stop looking eventually, so a fixture stuck in a non-final status cannot keep
    // costing requests forever
    public static final Duration LATEST_CHECK_AFTER_KICKOFF = Duration.ofHours(8);

    // pause between consecutive rapid api calls in a polling batch
    public static final long RESULTS_RATE_LIMIT_MS = 10_000L;

    // how often the poll runs; also the width of the window used to spot fixtures that just aged out
    public static final Duration POLL_INTERVAL = Duration.ofMinutes(30);

    // the api accepts at most this many ids in one ?ids= call (verified: 21 is rejected)
    public static final int MAX_IDS_PER_REQUEST = 20;

    // a player who played (minutes > 0) but has no usable api rating still counts as an appearance,
    // rated at this neutral value
    public static final double DEFAULT_MATCH_RATING = 6.5;

    private static Set<String> buildExcluded() {
        HashSet<String> excluded = new HashSet<>(FINISHED_STATUSES);
        excluded.addAll(DEAD_STATUSES);
        return Set.copyOf(excluded);
    }
}
