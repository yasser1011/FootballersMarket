package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.client.FixtureDiscoveryClient;
import com.yasser.footballersmarket.fixture.client.LeagueStandingsClient;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.tournament.LeagueTeam;
import com.yasser.footballersmarket.fixture.tournament.Tournament;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FixtureDiscoveryService {

    private final FixtureDiscoveryClient fixtureDiscoveryClient;
    private final LeagueStandingsClient leagueStandingsClient;
    private final TournamentCatalog tournamentCatalog;
    private final FixtureSavingService fixtureSavingService;
    private final Logger logger = LoggerFactory.getLogger(FixtureDiscoveryService.class);

    // must match the @Scheduled zone below: at 01:00 Istanbul the UTC date is still the previous
    // day, so taking the window in UTC would fetch yesterday's and today's fixtures
    private static final String ZONE_STR =  "Europe/Istanbul";
    private static final ZoneId ZONE = ZoneId.of(ZONE_STR);

    // pause between tournaments so the daily run does not burst the rapid api quota. matches
    // WorldCupConstants.RESULTS_RATE_LIMIT_MS; 6 tournaments means 5 pauses, so ~50s per run
    private static final long DISCOVERY_RATE_LIMIT_MS = 10_000L;

    public FixtureDiscoveryService(FixtureDiscoveryClient fixtureDiscoveryClient, TournamentCatalog tournamentCatalog,
                                   LeagueStandingsClient leagueStandingsClient,
                                   FixtureSavingService fixtureSavingService){
        this.fixtureDiscoveryClient = fixtureDiscoveryClient;
        this.tournamentCatalog = tournamentCatalog;
        this.leagueStandingsClient = leagueStandingsClient;
        this.fixtureSavingService = fixtureSavingService;
    }

    // find fixtures of today and next day for selected leagues
    // try to find also the top 4 ranked teams of each league
    // English, Spanish, Italian, German, French, Champions League
    @Scheduled(cron = "0 0 1 * * *", zone = ZONE_STR)
    public List<FixtureResponseDto> discoverNewFixtures() throws InterruptedException {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);

        List<FixtureResponseDto> fixturesResult = new ArrayList<>();
        // team id -> league position, captured from the standings call we already make. frozen onto
        // each fixture row so settlement scores against the table as it stood at discovery
        Map<Long, Integer> positionByTeamId = new HashMap<>();
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

                LeagueTeam.Team[] leagueTopTeams = new LeagueTeam.Team[0];
                if (tournament.isStanding()) {
                    List<Long> orderedTeamIds = leagueStandingsClient.fetchOrderedTeamIds(tournament.getId());
                    for (int position = 0; position < orderedTeamIds.size(); position++) {
                        positionByTeamId.put(orderedTeamIds.get(position), position + 1);
                    }
                    leagueTopTeams = orderedTeamIds.stream()
                            .map(LeagueTeam.Team::new)
                            .toArray(LeagueTeam.Team[]::new);
                }

                fixturesResult.add(tournament.filterImportantFixtures(fixtureResponseDto, leagueTopTeams));
            } catch (Exception e) {
                logger.error("fixture discovery failed for tournament {}: {}",
                        tournament.getId(), e.getMessage());
            }
        }

        // saved as one batch after the loop: a save failure loses nothing permanently, since the
        // window overlaps the next run and inserting is skipped for fixtures already stored
        try {
            fixtureSavingService.saveNewFixtures(fixturesResult, positionByTeamId);
        } catch (Exception e) {
            logger.error("fixture discovery: saving failed: {}", e.getMessage());
        }

        return fixturesResult;
    }

    // fetch fixtures result schedulers


}
