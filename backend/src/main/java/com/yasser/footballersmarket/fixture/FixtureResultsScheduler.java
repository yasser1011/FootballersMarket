package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.client.FixtureResultsClient;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class FixtureResultsScheduler {

    private final FixtureRepository fixtureRepository;
    private final FixtureResultsClient fixtureResultsClient;
    private final FixtureResultsService fixtureResultsService;
    private final Logger logger = LoggerFactory.getLogger(FixtureResultsScheduler.class);

    public FixtureResultsScheduler(FixtureRepository fixtureRepository,
                                   FixtureResultsClient fixtureResultsClient,
                                   FixtureResultsService fixtureResultsService) {
        this.fixtureRepository = fixtureRepository;
        this.fixtureResultsClient = fixtureResultsClient;
        this.fixtureResultsService = fixtureResultsService;
    }

    // every 30 minutes on the clock. cron (not fixedDelay) so it never fires at context startup,
    // which keeps it from making live api calls during the short-lived integration test contexts.
    //
    // fixtures are fetched by id in batches of up to 20 (only the by-id call carries the per-player
    // minutes and ratings), so a busy matchday costs a couple of requests instead of one per
    // fixture. a fixture is only checked once the match should already be over, and there is no
    // retry list: whatever is still unfinished -- not returned, or returned mid-match -- simply
    // matches the query again on the next run
    @Scheduled(cron = "0 */30 * * * *", zone = "Europe/Istanbul")
    public void pollFixtureResults() throws InterruptedException {
        Instant now = Instant.now();
        List<Fixture> candidates = fixtureRepository.findPollable(
                FixtureResultConstants.EXCLUDED_FROM_RESULTS_POLL,
                now.minus(FixtureResultConstants.LATEST_CHECK_AFTER_KICKOFF),
                now.minus(FixtureResultConstants.EARLIEST_CHECK_AFTER_KICKOFF));
        warnAboutAgedOut(now);
        if (candidates.isEmpty()) return;

        logger.info("fixture results: checking {} fixture(s)", candidates.size());

        int batchSize = FixtureResultConstants.MAX_IDS_PER_REQUEST;
        for (int start = 0; start < candidates.size(); start += batchSize) {
            // rate-limit between rapid api calls. outside the try so an interrupt is not swallowed
            if (start > 0) pauseBetweenRequests();

            List<Fixture> batch = candidates.subList(start, Math.min(start + batchSize, candidates.size()));
            Map<Long, FixtureResponseDto.Response> results = fixtureResultsClient.fetchFixtureResults(
                    batch.stream().map(Fixture::getId).toList());

            for (Fixture fixture : batch) {
                FixtureResponseDto.Response result = results.get(fixture.getId());
                if (result == null) {
                    logger.warn("fixture results: no result returned for fixture {}, will be checked again",
                            fixture.getId());
                    continue;
                }
                // best-effort per fixture: one failure must not abort the rest of the batch
                try {
                    fixtureResultsService.applyResult(fixture, result);
                } catch (Exception e) {
                    logger.error("fixture results: error processing fixture {}: {}",
                            fixture.getId(), e.getMessage());
                }
            }
        }
    }

    // fixtures whose polling window closed since the previous run and which never reached a final
    // status. the window is one poll interval wide, so each such fixture is reported exactly once
    // rather than on every run forever
    private void warnAboutAgedOut(Instant now) {
        Instant windowEnd = now.minus(FixtureResultConstants.LATEST_CHECK_AFTER_KICKOFF);
        List<Fixture> agedOut = fixtureRepository.findPollable(
                FixtureResultConstants.EXCLUDED_FROM_RESULTS_POLL,
                windowEnd.minus(FixtureResultConstants.POLL_INTERVAL),
                windowEnd);
        for (Fixture fixture : agedOut) {
            logger.warn("fixture results: fixture {} never finished (status {}) and is no longer polled",
                    fixture.getId(), fixture.getStatus());
        }
    }

    // its own method so a test can skip the real ten second wait
    void pauseBetweenRequests() throws InterruptedException {
        Thread.sleep(FixtureResultConstants.RESULTS_RATE_LIMIT_MS);
    }
}
