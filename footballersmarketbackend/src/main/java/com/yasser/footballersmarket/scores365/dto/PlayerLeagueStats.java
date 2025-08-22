package com.yasser.footballersmarket.scores365.dto;

public class PlayerLeagueStats {
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private Double rating;

    public PlayerLeagueStats(){}

    public PlayerLeagueStats(Integer appearances, Integer goals, Integer assists, Double rating) {
        this.appearances = appearances;
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
    }

    public Integer getAppearances() {
        return appearances;
    }

    public Integer getGoals() {
        return goals;
    }

    public Integer getAssists() {
        return assists;
    }

    public Double getRating() {
        return rating;
    }


}
