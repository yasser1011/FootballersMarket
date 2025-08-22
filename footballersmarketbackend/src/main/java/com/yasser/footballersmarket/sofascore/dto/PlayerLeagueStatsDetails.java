package com.yasser.footballersmarket.sofascore.dto;

import java.util.List;

public class PlayerLeagueStatsDetails {
    private Double rating;
    private Integer goals;
    // goals + assists
    private Integer goalsAssistsSum;
    private Integer countRating;


    public PlayerLeagueStatsDetails(){}

    public PlayerLeagueStatsDetails(Double rating, Integer goals, Integer goalsAssistsSum, Integer countRating) {
        this.rating = rating;
        this.goals = goals;
        this.goalsAssistsSum = goalsAssistsSum;
        this.countRating = countRating;
    }

    public Double getRating() {
        return rating;
    }

    public Integer getGoals() {
        return goals;
    }

    public Integer getGoalsAssistsSum() {
        return goalsAssistsSum;
    }

    public Integer getCountRating() {
        return countRating;
    }
}
