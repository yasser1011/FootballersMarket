package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.tournament.Tournament;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class FixtureDiscoveryService {

    private final FixtureDiscoveryClient fixtureDiscoveryClient;
    private final TournamentCatalog tournamentCatalog;
    private final Logger logger = LoggerFactory.getLogger(FixtureDiscoveryService.class);

    // must match the @Scheduled zone below: at 01:00 Istanbul the UTC date is still the previous
    // day, so taking the window in UTC would fetch yesterday's and today's fixtures
    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    // pause between tournaments so the daily run does not burst the rapid api quota. matches
    // WorldCupConstants.RESULTS_RATE_LIMIT_MS; 6 tournaments means 5 pauses, so ~50s per run
    private static final long DISCOVERY_RATE_LIMIT_MS = 10_000L;

    public FixtureDiscoveryService(FixtureDiscoveryClient fixtureDiscoveryClient, TournamentCatalog tournamentCatalog){
        this.fixtureDiscoveryClient = fixtureDiscoveryClient;
        this.tournamentCatalog = tournamentCatalog;
    }

    // find fixtures of today and next day for selected leagues
    // try to find also the top 4 ranked teams of each league
    // English, Spanish, Italian, German, French, Champions League
    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Istanbul")
    public List<FixtureResponseDto> discoverNewFixtures() throws InterruptedException {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);

        List<FixtureResponseDto> fixturesResult = new ArrayList<>();
        List<Tournament> tournaments = tournamentCatalog.activeTournaments();

        for(int i = 0; i < tournaments.size(); i++){
            // rate-limit between rapid api calls. kept outside the try so an interrupt is not
            // swallowed, and skipped on the first pass so no time is wasted before any work
            if (i > 0) Thread.sleep(DISCOVERY_RATE_LIMIT_MS);

            Tournament tournament = tournaments.get(i);
            // best-effort per tournament: one failing league must not abort the remaining ones
            try {
                FixtureResponseDto fixtureResponseDto =
                        fixtureDiscoveryClient.fetchFixtures(tournament.getId(), today, tomorrow);

                fixturesResult.add(tournament.filterImportantFixtures(fixtureResponseDto));
            } catch (Exception e) {
                logger.error("fixture discovery failed for tournament {}: {}",
                        tournament.getId(), e.getMessage());
            }
        }
        return fixturesResult;
    }
}
