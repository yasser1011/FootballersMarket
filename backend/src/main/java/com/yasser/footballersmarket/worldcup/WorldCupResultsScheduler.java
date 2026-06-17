package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.FixtureResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// polls api-football for world cup fixtures that have kicked off but aren't finished yet,
// records their results and updates player stats + prediction settlement.
@Service
public class WorldCupResultsScheduler {
    private final WorldCupFixtureRepository fixtureRepository;
    private final WorldCupResultsClient resultsClient;
    private final WorldCupResultsService resultsService;
    private final Logger logger = LoggerFactory.getLogger(WorldCupResultsScheduler.class);

    public WorldCupResultsScheduler(WorldCupFixtureRepository fixtureRepository,
                                    WorldCupResultsClient resultsClient,
                                    WorldCupResultsService resultsService) {
        this.fixtureRepository = fixtureRepository;
        this.resultsClient = resultsClient;
        this.resultsService = resultsService;
    }

    // every 15 minutes on the clock. cron (not fixedDelay) so it never fires at context startup,
    // which keeps it from making live api calls during the short-lived integration test contexts.
    // candidates = kicked off (start < now) and not finished-or-dead -> live score/status updates
    // each tick and settlement when a match reaches a finished status. finished/dead statuses are
    // excluded so a settled match isn't re-polled (the settlement latch) and a postponed/abandoned
    // one isn't checked forever. includes yesterday's unfinished matches -> downtime self-heals.
    @Scheduled(cron = "0 */15 * * * *", zone = "Europe/Istanbul")
    public void pollFixtureResults() throws InterruptedException {
        Instant now = Instant.now();
        List<WorldCupFixture> candidates = fixtureRepository
                .findByStatusNotInAndDateBefore(WorldCupConstants.EXCLUDED_FROM_RESULTS_POLL, now);
        if (candidates.isEmpty()) return;

        logger.info("world cup results: polling {} in-play fixture(s)", candidates.size());
        for (WorldCupFixture fixture : candidates) {
            try {
                FixtureResult result = resultsClient.fetchFixtureResult(fixture.getId());
                if (result != null) {
                    resultsService.processFixtureResult(fixture, result);
                }
            } catch (Exception e) {
                logger.error("world cup results: error processing fixture {}: {}", fixture.getId(), e.getMessage());
            }
            // rate-limit between rapid api calls
            Thread.sleep(WorldCupConstants.RESULTS_RATE_LIMIT_MS);
        }
    }
}
