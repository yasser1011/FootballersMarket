package com.yasser.footballersmarket.rapidapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PlayerStats {
    private String team;
    private String teamLogo;
    private Integer numOfGames;
    private String position;
    // returned as string from Api
    private String rating;
    private Integer goals;
    private Integer assists;

    @SuppressWarnings("unchecked")
    @JsonProperty("team")
    private void getTeamName(Map<String,Object> team) {
        this.team = (String)team.get("name");
        this.teamLogo = (String)team.get("logo");
    }

    @SuppressWarnings("unchecked")
    @JsonProperty("games")
    private void getGamesData(Map<String,Object> games) {
        this.numOfGames = (Integer)games.get("appearences");
        this.position = (String)games.get("position");
        this.rating = (String) games.get("rating");
    }

    @SuppressWarnings("unchecked")
    @JsonProperty("goals")
    private void getGoalsData(Map<String,Object> goals) {
        this.goals = (Integer)goals.get("total");
        this.assists = (Integer)goals.get("assists");
    }

    public PlayerStats(){}

    public PlayerStats(String team, Integer numOfGames, String position, String rating, Integer goals, Integer assists) {
        this.team = team;
        this.numOfGames = numOfGames;
        this.position = position;
        this.rating = rating;
        this.goals = goals;
        this.assists = assists;
    }

    public PlayerStats(String team, String teamLogo, Integer numOfGames, String position, String rating, Integer goals, Integer assists) {
        this.team = team;
        this.teamLogo = teamLogo;
        this.numOfGames = numOfGames;
        this.position = position;
        this.rating = rating;
        this.goals = goals;
        this.assists = assists;
    }

    public String getTeam() {
        return team;
    }

    public Integer getNumOfGames() {
        return numOfGames;
    }

    public String getPosition() {
        return position;
    }

    public String getRating() {
        return rating;
    }

    public Integer getGoals() {
        return goals;
    }

    public Integer getAssists() {
        return assists;
    }

    public String getTeamLogo() {
        return teamLogo;
    }
}
