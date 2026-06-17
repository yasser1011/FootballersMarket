package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.FixtureResult;
import com.yasser.footballersmarket.worldcup.dto.WorldCupFixtureResponse;
import com.yasser.footballersmarket.worldcup.dto.WorldCupFixtureResponse.TeamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// serves the public "yesterday + today + tomorrow" fixtures view. db-first: returns seeded/persisted
// fixtures for the window; if none exist (e.g. a knockout day not seeded yet) it fetches the range
// from rapid once and persists it, so subsequent calls are served from the db.
@Service
public class WorldCupFixtureService {
    private final WorldCupFixtureRepository fixtureRepository;
    private final WorldCupTeamRepository teamRepository;
    private final WorldCupResultsClient resultsClient;
    private final Logger logger = LoggerFactory.getLogger(WorldCupFixtureService.class);

    // window is computed in the market's timezone (matches the results scheduler zone)
    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    public WorldCupFixtureService(WorldCupFixtureRepository fixtureRepository,
                                  WorldCupTeamRepository teamRepository,
                                  WorldCupResultsClient resultsClient) {
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
        this.resultsClient = resultsClient;
    }

    @Transactional
    public List<WorldCupFixtureResponse> getRecentAndUpcomingFixtures() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        Instant start = yesterday.atStartOfDay(ZONE).toInstant();
        Instant end = today.plusDays(2).atStartOfDay(ZONE).toInstant(); // exclusive: end of tomorrow

        List<WorldCupFixture> fixtures =
                fixtureRepository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(start, end);
        if (fixtures.isEmpty()) {
            fixtures = fetchAndPersist(yesterday, tomorrow, start, end);
        }
        return toResponses(fixtures);
    }

    // discovery (daily scheduler): insert any fixtures for [from, to] not already in the db.
    // INSERT-ONLY — never overwrites existing rows, so it can't wipe the live score/status/result
    // the results scheduler maintains. returns how many new fixtures were inserted.
    @Transactional
    public int discoverNewFixtures(LocalDate from, LocalDate to) {
        List<WorldCupFixture> fetched = new ArrayList<>();
        for (FixtureResult fr : resultsClient.fetchFixturesByDateRange(from, to)) {
            WorldCupFixture fixture = toEntity(fr);
            if (fixture != null) fetched.add(fixture);
        }
        if (fetched.isEmpty()) return 0;

        Set<Long> fetchedIds = fetched.stream().map(WorldCupFixture::getId).collect(Collectors.toSet());
        Set<Long> existingIds = fixtureRepository.findAllById(fetchedIds).stream()
                .map(WorldCupFixture::getId).collect(Collectors.toSet());
        List<WorldCupFixture> toInsert = fetched.stream()
                .filter(f -> !existingIds.contains(f.getId()))
                .collect(Collectors.toList());

        if (!toInsert.isEmpty()) fixtureRepository.saveAll(toInsert);
        logger.info("world cup discovery: {} fetched, {} new inserted for {}..{}",
                fetched.size(), toInsert.size(), from, to);
        return toInsert.size();
    }

    private List<WorldCupFixture> fetchAndPersist(LocalDate from, LocalDate to, Instant start, Instant end) {
        List<WorldCupFixture> toSave = new ArrayList<>();
        for (FixtureResult fr : resultsClient.fetchFixturesByDateRange(from, to)) {
            WorldCupFixture fixture = toEntity(fr);
            if (fixture != null) toSave.add(fixture);
        }
        if (toSave.isEmpty()) return List.of();

        fixtureRepository.saveAll(toSave);
        logger.info("world cup fixtures: persisted {} fixture(s) for {}..{}", toSave.size(), from, to);
        // keep only those actually inside the window, ordered by kickoff
        return toSave.stream()
                .filter(f -> f.getDate() != null && !f.getDate().isBefore(start) && f.getDate().isBefore(end))
                .sorted(Comparator.comparing(WorldCupFixture::getDate))
                .collect(Collectors.toList());
    }

    private WorldCupFixture toEntity(FixtureResult fr) {
        if (fr.fixture() == null || fr.fixture().id() == null || fr.fixture().date() == null || fr.teams() == null) {
            return null;
        }
        Long homeId = fr.teams().home() != null ? fr.teams().home().id() : null;
        Long awayId = fr.teams().away() != null ? fr.teams().away().id() : null;
        String round = fr.league() != null ? fr.league().round() : null;
        String status = fr.fixture().status() != null ? fr.fixture().status().shortCode() : null;
        Instant date = OffsetDateTime.parse(fr.fixture().date()).toInstant();
        return new WorldCupFixture(fr.fixture().id(), date, round, homeId, awayId, status);
    }

    private List<WorldCupFixtureResponse> toResponses(List<WorldCupFixture> fixtures) {
        Set<Long> teamIds = fixtures.stream()
                .flatMap(f -> Stream.of(f.getHomeTeamId(), f.getAwayTeamId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WorldCupTeam> teams = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(WorldCupTeam::getId, t -> t));

        return fixtures.stream()
                .map(f -> new WorldCupFixtureResponse(
                        f.getId(), f.getDate(), f.getRound(), f.getStatus(),
                        teamInfo(teams.get(f.getHomeTeamId())),
                        teamInfo(teams.get(f.getAwayTeamId())),
                        f.getHomeGoals(), f.getAwayGoals(), f.getWinnerTeamId(), f.getElapsed()))
                .collect(Collectors.toList());
    }

    private TeamInfo teamInfo(WorldCupTeam team) {
        if (team == null) return null;
        return new TeamInfo(team.getId(), team.getName(), team.getLogoUrl(), team.getFifaRank());
    }
}
