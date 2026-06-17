package com.yasser.footballersmarket.worldcup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

// discovers world cup fixtures once a day: range-fetches yesterday..tomorrow from rapid and inserts
// any not already in the db (insert-only — never clobbers live/result rows). keeps fixture rows
// present for the results scheduler and the public fixtures view without an api call per request.
@Service
public class WorldCupFixtureDiscoveryScheduler {
    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private final WorldCupFixtureService fixtureService;
    private final Logger logger = LoggerFactory.getLogger(WorldCupFixtureDiscoveryScheduler.class);

    public WorldCupFixtureDiscoveryScheduler(WorldCupFixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    // 00:10 every day. cron (not fixedDelay) so it never fires at context startup, keeping it from
    // making live api calls during the short-lived integration test contexts.
    @Scheduled(cron = "0 10 0 * * *", zone = "Europe/Istanbul")
    public void discoverFixtures() {
        LocalDate today = LocalDate.now(ZONE);
        try {
            fixtureService.discoverNewFixtures(today.minusDays(1), today.plusDays(1));
        } catch (Exception e) {
            logger.error("world cup discovery: failed: {}", e.getMessage());
        }
    }
}
