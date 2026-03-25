package com.yasser.footballersmarket.rapidapi.dto;

import java.util.HashMap;

public class SearchTeam {
    private final String leagueId;
    private final String seasonId;
    private final String teamId;

    public SearchTeam(String leagueId, String seasonId, String teamId) {
        this.leagueId = leagueId;
        this.seasonId = seasonId;
        this.teamId = teamId;
    }

    public HashMap<String, String> getSearchTeamInputParamsMap(){
        HashMap<String, String> searchInputParams = new HashMap<>();
        searchInputParams.put("league", leagueId);
        searchInputParams.put("season", seasonId);
        searchInputParams.put("team", teamId);
        return searchInputParams;
    }
}
