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

    // any minutes at all is an appearance. an unused sub is listed with 0 minutes and does not count
    static boolean appeared(FixtureResponseDto.Response.Games games) {
        return games != null && games.minutes() != null && games.minutes() > 0;
    }

    // the api rating when it parses and is positive, otherwise the neutral default. the api sends
    // the rating as a string and uses "0" or null when it has none
    static double matchRating(FixtureResponseDto.Response.Games games) {
        if (games == null || games.rating() == null) return FixtureResultConstants.DEFAULT_MATCH_RATING;
        try {
            double rating = Double.parseDouble(games.rating());
            return rating > 0 ? rating : FixtureResultConstants.DEFAULT_MATCH_RATING;
        } catch (NumberFormatException e) {
            return FixtureResultConstants.DEFAULT_MATCH_RATING;
        }
    }
}
