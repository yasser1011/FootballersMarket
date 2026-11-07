package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;

// shared reads of the api fixture payload. both the discovery save and the results poll write the
// same columns, so the interpretation lives in one place rather than being duplicated and drifting
final class FixtureApiMapping {
    private FixtureApiMapping() {}

    // the api winner flag marks who advanced, so it covers penalty wins that goals alone read as a
    // draw. both null/false = draw or undecided
    static Long resolveWinner(FixtureResponseDto.Response fixture) {
        if (fixture.teams() == null) return null;
        var home = fixture.teams().home();
        var away = fixture.teams().away();
        if (home != null && Boolean.TRUE.equals(home.winner())) return home.id();
        if (away != null && Boolean.TRUE.equals(away.winner())) return away.id();
        return null;
    }

    static String status(FixtureResponseDto.Response fixture) {
        return fixture.fixture() != null && fixture.fixture().status() != null
                ? fixture.fixture().status().shortName()
                : null;
    }

    static Integer homeGoals(FixtureResponseDto.Response fixture) {
        return fixture.goals() != null ? fixture.goals().home() : null;
    }

    static Integer awayGoals(FixtureResponseDto.Response fixture) {
        return fixture.goals() != null ? fixture.goals().away() : null;
    }
}
