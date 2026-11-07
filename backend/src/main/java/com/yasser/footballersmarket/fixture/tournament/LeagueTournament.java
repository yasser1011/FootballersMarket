package com.yasser.footballersmarket.fixture.tournament;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;

public class LeagueTournament extends Tournament {

    private final int topRankedTeamLimit;
    // number of clubs in the division, used to normalise a position gap to 0..1
    private final int leagueSize;
    private final LeagueTeam.Team[] leagueStandardTeams;
    private final Logger logger = LoggerFactory.getLogger(LeagueTournament.class);

    public LeagueTournament(Long leagueId, int topRankedTeamLimit, int leagueSize,
                            LeagueTeam.Team[] leagueStandardTeams) {
        super(leagueId);
        this.topRankedTeamLimit = topRankedTeamLimit;
        this.leagueSize = leagueSize;
        this.leagueStandardTeams = leagueStandardTeams;
    }

    @Override
    public boolean isStanding() {
        return true;
    }

    @Override
    public FixtureResponseDto filterImportantFixtures(FixtureResponseDto fixtureResponseDtoList, LeagueTeam.Team[] topRankedTeams) {
        if (fixtureResponseDtoList.response() == null) {
            return new FixtureResponseDto(new FixtureResponseDto.Response[0]);
        }

        LeagueTeam.Team[] mergedTeams = Stream.concat(
                Arrays.stream(topRankedTeams).limit(topRankedTeamLimit),
                Arrays.stream(leagueStandardTeams)
        ).distinct().toArray(LeagueTeam.Team[]::new);

        FixtureResponseDto.Response[] filteredResponse = Arrays.stream(fixtureResponseDtoList.response())
                .filter(fixture -> involvesTrackedTeam(fixture, mergedTeams))
                .toArray(FixtureResponseDto.Response[]::new);

        return new FixtureResponseDto(filteredResponse);
    }

    private boolean involvesTrackedTeam(FixtureResponseDto.Response fixture, LeagueTeam.Team[] teams) {
        if (fixture.teams() == null) return false;
        Long homeId = fixture.teams().home() != null ? fixture.teams().home().id() : null;
        Long awayId = fixture.teams().away() != null ? fixture.teams().away().id() : null;
        return Arrays.stream(teams)
                .anyMatch(team -> team.id().equals(homeId) || team.id().equals(awayId));
    }


    public int getLeagueSize() {
        return leagueSize;
    }
}
