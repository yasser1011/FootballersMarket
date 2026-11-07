package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FixtureSavingService {

    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final Logger logger = LoggerFactory.getLogger(FixtureSavingService.class);

    public FixtureSavingService(FixtureRepository fixtureRepository,
                                TeamRepository teamRepository) {
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
    }

    // two writes that belong together, hence the transaction: teams are upserted so a fixture never
    // references a club we have no row for, and new fixtures are inserted. returns inserted count.
    @Transactional
    public int saveNewFixtures(List<FixtureResponseDto> tournamentResponses,
                               Map<Long, Integer> positionByTeamId) {
        List<FixtureResponseDto.Response> fixtures = flatten(tournamentResponses);
        if (fixtures.isEmpty()) return 0;

        teamRepository.saveAll(teamsReferencedBy(fixtures));

        List<Fixture> toInsert = newFixturesOnly(fixtures, positionByTeamId);
        if (!toInsert.isEmpty()) fixtureRepository.saveAll(toInsert);

        logger.info("fixture saving: {} fetched, {} new inserted", fixtures.size(), toInsert.size());
        return toInsert.size();
    }

    private List<FixtureResponseDto.Response> flatten(List<FixtureResponseDto> tournamentResponses) {
        List<FixtureResponseDto.Response> fixtures = new ArrayList<>();
        for (FixtureResponseDto response : tournamentResponses) {
            if (response == null || response.response() == null) continue;
            for (FixtureResponseDto.Response fixture : response.response()) {
                if (isUsable(fixture)) fixtures.add(fixture);
            }
        }
        return fixtures;
    }

    private boolean isUsable(FixtureResponseDto.Response fixture) {
        return fixture != null
                && fixture.fixture() != null
                && fixture.fixture().id() != null
                && fixture.fixture().date() != null
                && fixture.teams() != null
                && fixture.teams().home() != null && fixture.teams().home().id() != null
                && fixture.teams().away() != null && fixture.teams().away().id() != null;
    }

    // deduped by id: one club appears in many fixtures, so the same team would otherwise be
    // written once per fixture it plays in
    private List<Team> teamsReferencedBy(List<FixtureResponseDto.Response> fixtures) {
        Map<Long, Team> teams = new HashMap<>();
        for (FixtureResponseDto.Response fixture : fixtures) {
            var home = fixture.teams().home();
            var away = fixture.teams().away();
            teams.putIfAbsent(home.id(), new Team(home.id(), home.name(), home.logo()));
            teams.putIfAbsent(away.id(), new Team(away.id(), away.name(), away.logo()));
        }
        return new ArrayList<>(teams.values());
    }

    private List<Fixture> newFixturesOnly(List<FixtureResponseDto.Response> fixtures,
                                          Map<Long, Integer> positionByTeamId) {
        Map<Long, Fixture> byId = new HashMap<>();
        for (FixtureResponseDto.Response fixture : fixtures) {
            byId.putIfAbsent(fixture.fixture().id(), toEntity(fixture, positionByTeamId));
        }

        Set<Long> existingIds = fixtureRepository.findAllById(byId.keySet()).stream()
                .map(Fixture::getId)
                .collect(Collectors.toSet());

        return byId.values().stream()
                .filter(fixture -> !existingIds.contains(fixture.getId()))
                .collect(Collectors.toList());
    }

    private Fixture toEntity(FixtureResponseDto.Response fixture,
                             Map<Long, Integer> positionByTeamId) {
        var home = fixture.teams().home();
        var away = fixture.teams().away();
        Instant date = OffsetDateTime.parse(fixture.fixture().date()).toInstant();
        Long leagueId = fixture.league() != null ? fixture.league().id() : null;
        String round = fixture.league() != null ? fixture.league().round() : null;

        Fixture entity = new Fixture(fixture.fixture().id(), date, leagueId, round,
                FixtureApiMapping.status(fixture), home.id(), away.id(),
                FixtureApiMapping.homeGoals(fixture), FixtureApiMapping.awayGoals(fixture),
                FixtureApiMapping.resolveWinner(fixture));

        // null for a club whose table we do not track (champions league sides from other leagues)
        entity.setHomeTeamPosition(positionByTeamId.get(home.id()));
        entity.setAwayTeamPosition(positionByTeamId.get(away.id()));
        return entity;
    }

}
