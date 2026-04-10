package com.yasser.footballersmarket.rapidapi;

import com.yasser.footballersmarket.rapidapi.dto.SearchTeam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;

@Configuration
// Spring does not allow injecting to static fields
// also class is singleton no need to have static fields
public class RapidApiConfig {
    private String SPANISH_LEAGUE_ID = "140";
    private String ENGLISH_LEAGUE_ID = "39";
    private String ITALIAN_LEAGUE_ID = "135";
    private String FRENCH_LEAGUE_ID = "61";
    private String GERMAN_LEAGUE_ID = "78";
    private String LIVERPOOL_ID = "40";
    private String ARSENAL_ID = "42";
    private String MANCITY_ID = "50";
    private String CHELSEA_ID = "49";
    private String MANUTD_ID = "33";
    private String TOTTENHAM_ID = "47";
    private String INTER_MILAN_ID = "505";
    private String AC_MILAN_ID = "489";
    private String NAPOLI_ID = "492";
    private String JUVENTUS_ID = "496";
    private String ATLETICO_MADRID_ID = "530";
    private String BARCELONA_ID = "529";
    private String REAL_MADRID_ID = "541";
    private String BAYERN_ID = "157";
    private String PARIS_ID = "85";

    @Value("${rapidApiUrl}")
    private String rapidApiUrl;

    @Value("${x-rapidapi-host}")
    private String rapidApiHost;

    @Value("${x-rapidapi-key}")
    private String rapidApiKey;

    @Value("${rapidapi-season}")
    private String rapidSearchSeason;

    public String getRapidApiUrl() {
        return rapidApiUrl;
    }

    public String getRapidApiHost() {
        return rapidApiHost;
    }

    public String getRapidApiKey() {
        return rapidApiKey;
    }


    public List<HashMap<String, String>> getSchedulerTeamsSearchInput(){
        return List.of(
//                new SearchTeam(ENGLISH_LEAGUE_ID, rapidSearchSeason, CHELSEA_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(SPANISH_LEAGUE_ID, rapidSearchSeason, BARCELONA_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ENGLISH_LEAGUE_ID, rapidSearchSeason, MANCITY_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ENGLISH_LEAGUE_ID, rapidSearchSeason, LIVERPOOL_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ENGLISH_LEAGUE_ID, rapidSearchSeason, ARSENAL_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ENGLISH_LEAGUE_ID, rapidSearchSeason, MANUTD_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ENGLISH_LEAGUE_ID, rapidSearchSeason, TOTTENHAM_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ITALIAN_LEAGUE_ID, rapidSearchSeason, INTER_MILAN_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ITALIAN_LEAGUE_ID, rapidSearchSeason, AC_MILAN_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ITALIAN_LEAGUE_ID, rapidSearchSeason, NAPOLI_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(ITALIAN_LEAGUE_ID, rapidSearchSeason, JUVENTUS_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(SPANISH_LEAGUE_ID, rapidSearchSeason, ATLETICO_MADRID_ID).getSearchTeamInputParamsMap(),
//                new SearchTeam(SPANISH_LEAGUE_ID, rapidSearchSeason, REAL_MADRID_ID).getSearchTeamInputParamsMap(),
                new SearchTeam(GERMAN_LEAGUE_ID, rapidSearchSeason, BAYERN_ID).getSearchTeamInputParamsMap()
//                new SearchTeam(FRENCH_LEAGUE_ID, rapidSearchSeason, PARIS_ID).getSearchTeamInputParamsMap()
        );
    }
}
