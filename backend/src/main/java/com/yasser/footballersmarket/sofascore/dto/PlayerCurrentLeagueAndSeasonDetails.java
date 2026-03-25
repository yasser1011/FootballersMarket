package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class PlayerCurrentLeagueAndSeasonDetails {
    private String leagueName;
    private Integer leagueId;
    private String seasonName;
    private Integer seasonId;

    public PlayerCurrentLeagueAndSeasonDetails(){}

    public PlayerCurrentLeagueAndSeasonDetails(String leagueName, Integer leagueId, String seasonName, Integer seasonId) {
        this.leagueName = leagueName;
        this.leagueId = leagueId;
        this.seasonName = seasonName;
        this.seasonId = seasonId;
    }

    @JsonProperty("uniqueTournament")
    private void getLeagueDetails(Map<String,Object> league) {
        this.leagueName = (String)league.get("name");
        this.leagueId = (Integer) league.get("id");
    }

    @JsonProperty("seasons")
    private void getSeasonDetails(List<Map<String,Object>> seasons) {
        Map<String,Object> currentSeason = seasons.get(0);
        this.seasonName = (String)currentSeason.get("name");
        this.seasonId = (Integer) currentSeason.get("id");
    }

    public String getLeagueName() {
        return leagueName;
    }

    public Integer getLeagueId() {
        return leagueId;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public Integer getSeasonId() {
        return seasonId;
    }
}
