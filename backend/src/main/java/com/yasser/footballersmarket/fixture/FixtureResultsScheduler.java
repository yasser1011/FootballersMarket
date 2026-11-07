package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.client.FixtureResultsClient;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

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
    // one request per fixture, because only the single-fixture call carries the per-player match
    // ratings. the api budget is 100 requests a day and discovery already spends 11, but a fixture
    // is only checked once the match should already be over -- so a finished match costs ~1 request
    // rather than being polled throughout, and cycles with nothing to check cost nothing.
    @Scheduled(cron = "0 */30 * * * *", zone = "Europe/Istanbul")
    public void pollFixtureResults() throws InterruptedException {
        Instant now = Instant.now();
        List<Fixture> candidates = fixtureRepository.findPollable(
                FixtureResultConstants.EXCLUDED_FROM_RESULTS_POLL,
                now.minus(FixtureResultConstants.LATEST_CHECK_AFTER_KICKOFF),
                now.minus(FixtureResultConstants.EARLIEST_CHECK_AFTER_KICKOFF));
        if (candidates.isEmpty()) return;

        logger.info("fixture results: checking {} fixture(s)", candidates.size());

        boolean firstFixture = true;
        for (Fixture fixture : candidates) {
            // rate-limit between rapid api calls. outside the try so an interrupt is not swallowed
            if (!firstFixture) Thread.sleep(FixtureResultConstants.RESULTS_RATE_LIMIT_MS);
            firstFixture = false;

            // best-effort per fixture: one failure must not abort the rest of the batch
            try {
                FixtureResponseDto.Response result = fixtureResultsClient.fetchFixtureResult(fixture.getId());
                if (result != null) {
                    fixtureResultsService.applyResult(fixture, result);
                }
            } catch (Exception e) {
                logger.error("fixture results: error processing fixture {}: {}",
                        fixture.getId(), e.getMessage());
            }
        }
    }
}
