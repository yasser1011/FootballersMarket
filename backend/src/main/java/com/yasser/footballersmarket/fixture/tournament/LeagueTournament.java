package com.yasser.footballersmarket.fixture.tournament;

import com.yasser.footballersmarket.fixture.LeagueStandingsClient;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.LeagueStandingsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;

public class LeagueTournament extends Tournament {

    private final int topRankedTeamLimit;
    private final LeagueStandingsClient leagueStandingsClient;
    private final LeagueTeam.Team[] leagueStandardTeams;
    private final Logger logger = LoggerFactory.getLogger(LeagueTournament.class);

    public LeagueTournament(Long leagueId, int topRankedTeamLimit, LeagueStandingsClient leagueStandingsClient,
                            LeagueTeam.Team[] teams) {
        super(leagueId);
        this.topRankedTeamLimit = topRankedTeamLimit;
        this.leagueStandingsClient = leagueStandingsClient;
        this.leagueStandardTeams = teams;
    }

    @Override
    public FixtureResponseDto filterImportantFixtures(FixtureResponseDto fixtureResponseDtoList) {
        if (fixtureResponseDtoList.response() == null) {
            return new FixtureResponseDto(new FixtureResponseDto.Response[0]);
        }

        LeagueTeam.Team[] mergedTeams = Stream.concat(
                Arrays.stream(getTopRankedTeams()),
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

    // fetches the top ranked teams from external service. an empty result is not fatal: filtering
    // falls back to leagueStandardTeams, so a standings outage still yields the usual fixtures
    private LeagueTeam.Team[] getTopRankedTeams(){
        LeagueStandingsDto leagueStandingsResponseDto = leagueStandingsClient.fetchLeagueStandings(getId());

        if (leagueStandingsResponseDto.response() == null || leagueStandingsResponseDto.response().length == 0) {
            logger.warn("standings: empty response for league {}", getId());
            return new LeagueTeam.Team[0];
        }
        LeagueStandingsDto.Response.League league = leagueStandingsResponseDto.response()[0].league();
        if (league == null || league.standings() == null || league.standings().length == 0) {
            logger.warn("standings: no table for league {}", getId());
            return new LeagueTeam.Team[0];
        }
        LeagueStandingsDto.Response.League.Standing[] table = league.standings()[0];
        if (table == null) {
            logger.warn("standings: null table for league {}", getId());
            return new LeagueTeam.Team[0];
        }

        return Arrays.stream(table)
                .filter(standing -> standing.team() != null && standing.team().id() != null)
                .limit(topRankedTeamLimit)
                .map(standing -> new LeagueTeam.Team(standing.team().id()))
                .toArray(LeagueTeam.Team[]::new);
    }
}
