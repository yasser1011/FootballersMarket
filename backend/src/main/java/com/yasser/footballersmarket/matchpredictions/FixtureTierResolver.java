package com.yasser.footballersmarket.matchpredictions;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import org.springframework.stereotype.Component;

import java.util.OptionalInt;

// turns a fixture's frozen position snapshot plus the tracked-team list into the two numbers the
// reward calculator needs: a tier (how important the fixture is) and a normalised position gap
@Component
public class FixtureTierResolver {

    private static final int TOP_POSITION_LIMIT = 4;
    private static final Long CHAMPIONS_LEAGUE_ID = 2L;

    // how important one side is
    private enum TeamLevel { REGULAR, TRACKED, TOP }

    // tier by the pair of levels. deliberately NOT a sum: TRACKED+TRACKED (an even contest between
    // two decent sides) outranks TOP+REGULAR (a mismatch), which a sum would tie at 4
    private static final int[][] TIER_BY_LEVELS = {
            //            REGULAR  TRACKED  TOP
            /* REGULAR */ {0,       1,      2},
            /* TRACKED */ {1,       3,      4},
            /* TOP     */ {2,       4,      5},
    };

    private final TournamentCatalog tournamentCatalog;

    public FixtureTierResolver(TournamentCatalog tournamentCatalog) {
        this.tournamentCatalog = tournamentCatalog;
    }

    public record FixtureWeighting(int tier, Double gapNorm, boolean championsLeague) {}

    public FixtureWeighting resolve(Fixture fixture) {
        TeamLevel home = levelOf(fixture.getHomeTeamId(), fixture.getHomeTeamPosition());
        TeamLevel away = levelOf(fixture.getAwayTeamId(), fixture.getAwayTeamPosition());

        return new FixtureWeighting(
                TIER_BY_LEVELS[home.ordinal()][away.ordinal()],
                gapNorm(fixture),
                CHAMPIONS_LEAGUE_ID.equals(fixture.getLeagueId()));
    }

    private TeamLevel levelOf(Long teamId, Integer position) {
        if (position != null && position <= TOP_POSITION_LIMIT) return TeamLevel.TOP;
        if (tournamentCatalog.isTrackedTeam(teamId)) return TeamLevel.TRACKED;
        return TeamLevel.REGULAR;
    }

    // null when either side has no stored position, or the competition has no table to size the gap
    // against (champions league) -> the calculator falls back to the tier base alone
    private Double gapNorm(Fixture fixture) {
        Integer home = fixture.getHomeTeamPosition();
        Integer away = fixture.getAwayTeamPosition();
        if (home == null || away == null) return null;

        OptionalInt leagueSize = tournamentCatalog.leagueSize(fixture.getLeagueId());
        if (leagueSize.isEmpty() || leagueSize.getAsInt() <= 1) return null;

        return Math.abs(home - away) / (double) (leagueSize.getAsInt() - 1);
    }
}
