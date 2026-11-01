package com.yasser.footballersmarket.fixture.tournament;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;

import java.util.Set;

public abstract class Tournament {
    private final Long id;

    public Tournament(Long id){
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public abstract boolean isStanding();

    public abstract FixtureResponseDto filterImportantFixtures(FixtureResponseDto allTournamentFixtures, LeagueTeam.Team[] topRankedTeams);
}
