package com.yasser.footballersmarket.fixture.tournament;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;

public class KnockoutTournament extends Tournament {
    public KnockoutTournament(Long id) {
        super(id);
    }

    @Override
    public FixtureResponseDto filterImportantFixtures(FixtureResponseDto allTournamentFixtures) {
        // every knockout fixture matters, and there is no league table to rank teams by
        return allTournamentFixtures;
    }
}
