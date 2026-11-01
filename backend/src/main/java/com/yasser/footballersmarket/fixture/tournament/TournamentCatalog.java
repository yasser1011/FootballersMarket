package com.yasser.footballersmarket.fixture.tournament;

import com.yasser.footballersmarket.fixture.LeagueStandingsClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// the tournaments fixture discovery runs over. built once at startup: this is fixed configuration,
// not something that can differ between calls
@Component
public class TournamentCatalog {

    // English, Spanish, Italian, German, French, Champions League
    private static final Long SPANISH_LEAGUE_ID = 140L;
    private static final Long ENGLISH_LEAGUE_ID = 39L;
    private static final Long ITALIAN_LEAGUE_ID = 135L;
    private static final Long FRENCH_LEAGUE_ID = 61L;
    private static final Long GERMAN_LEAGUE_ID = 78L;
    private static final Long CHAMPIONS_LEAGUE_ID = 2L;

    private static final Long LIVERPOOL_ID = 40L;
    private static final Long ARSENAL_ID = 42L;
    private static final Long MANCITY_ID = 50L;
    private static final Long CHELSEA_ID = 49L;
    private static final Long MANUTD_ID = 33L;
    private static final Long TOTTENHAM_ID = 47L;
    private static final Long INTER_MILAN_ID = 505L;
    private static final Long AC_MILAN_ID = 489L;
    private static final Long NAPOLI_ID = 492L;
    private static final Long JUVENTUS_ID = 496L;
    private static final Long ATLETICO_MADRID_ID = 530L;
    private static final Long BARCELONA_ID = 529L;
    private static final Long REAL_MADRID_ID = 541L;
    private static final Long BAYERN_ID = 157L;
    private static final Long PARIS_ID = 85L;

    // teams always tracked regardless of where they currently sit in the table
    private static final Map<Long, LeagueTeam.Team[]> LEAGUE_TO_TEAMS = Map.of(
            SPANISH_LEAGUE_ID, new LeagueTeam.Team[]{new LeagueTeam.Team(ATLETICO_MADRID_ID), new LeagueTeam.Team(BARCELONA_ID),
                    new LeagueTeam.Team(REAL_MADRID_ID)},
            ENGLISH_LEAGUE_ID, new LeagueTeam.Team[]{new LeagueTeam.Team(LIVERPOOL_ID), new LeagueTeam.Team(ARSENAL_ID),
                    new LeagueTeam.Team(MANCITY_ID), new LeagueTeam.Team(MANUTD_ID), new LeagueTeam.Team(TOTTENHAM_ID),
                    new LeagueTeam.Team(CHELSEA_ID)},
            ITALIAN_LEAGUE_ID, new LeagueTeam.Team[]{new LeagueTeam.Team(INTER_MILAN_ID), new LeagueTeam.Team(AC_MILAN_ID),
                    new LeagueTeam.Team(NAPOLI_ID), new LeagueTeam.Team(JUVENTUS_ID)},
            GERMAN_LEAGUE_ID, new LeagueTeam.Team[]{new LeagueTeam.Team(BAYERN_ID)},
            FRENCH_LEAGUE_ID, new LeagueTeam.Team[]{new LeagueTeam.Team(PARIS_ID)}
    );

    private final List<Tournament> activeTournaments;

    public TournamentCatalog(){
        this.activeTournaments = List.of(
                new LeagueTournament(SPANISH_LEAGUE_ID, 3, LEAGUE_TO_TEAMS.get(SPANISH_LEAGUE_ID)),
                new LeagueTournament(ENGLISH_LEAGUE_ID, 4, LEAGUE_TO_TEAMS.get(ENGLISH_LEAGUE_ID)),
                new LeagueTournament(ITALIAN_LEAGUE_ID, 4, LEAGUE_TO_TEAMS.get(ITALIAN_LEAGUE_ID)),
                new LeagueTournament(GERMAN_LEAGUE_ID, 3, LEAGUE_TO_TEAMS.get(GERMAN_LEAGUE_ID)),
                new LeagueTournament(FRENCH_LEAGUE_ID, 2, LEAGUE_TO_TEAMS.get(FRENCH_LEAGUE_ID)),
                new KnockoutTournament(CHAMPIONS_LEAGUE_ID)
        );
    }

    public List<Tournament> activeTournaments() {
        return activeTournaments;
    }
}
