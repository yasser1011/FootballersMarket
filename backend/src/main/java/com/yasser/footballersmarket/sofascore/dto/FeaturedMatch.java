package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FeaturedMatch {
    private String homeTeamName;
    private String homeTeamImgUrl;
    private String awayTeamName;
    private String awayTeamImgUrl;
    private String status;
    private LocalDateTime startDatetime;

    public FeaturedMatch(){}

    public FeaturedMatch(String homeTeamName, String homeTeamImg, String awayTeamName, String awayTeamImg, String status, LocalDateTime startDatetime) {
        this.homeTeamName = homeTeamName;
        this.homeTeamImgUrl = homeTeamImg;
        this.awayTeamName = awayTeamName;
        this.awayTeamImgUrl = awayTeamImg;
        this.status = status;
        this.startDatetime = startDatetime;
    }

    @JsonProperty("featuredEvents")
    private void getMatchEventDetails(List<Map<String,Object>> featuredEvents) {
        Map<String,Object> featuredEvent = featuredEvents.get(0);
        Integer startTimestamp = (Integer) featuredEvent.get("startTimestamp");
        long l = TimeUnit.SECONDS.toMillis(startTimestamp);
        Instant instant = Instant.ofEpochMilli(l);
        this.startDatetime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        Map<String,Object> statusObj = (Map<String,Object>)featuredEvent.get("status");
        this.status = (String)statusObj.get("type");

        Map<String,Object> homeTeamObj = (Map<String,Object>)featuredEvent.get("homeTeam");
        this.homeTeamName = (String)homeTeamObj.get("name");
        Integer homeTeamId = (Integer)homeTeamObj.get("id");
        this.homeTeamImgUrl = "/team/"+ homeTeamId +"/image";

        Map<String,Object> awayTeamObj = (Map<String,Object>)featuredEvent.get("awayTeam");
        this.awayTeamName = (String)awayTeamObj.get("name");
        Integer awayTeamId = (Integer)awayTeamObj.get("id");
        this.awayTeamImgUrl = "/team/"+ awayTeamId +"/image";

    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public String getHomeTeamImgUrl() {
        return homeTeamImgUrl;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public String getAwayTeamImgUrl() {
        return awayTeamImgUrl;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }

    public void setHomeTeamImgUrl(String homeTeamImgUrl) {
        this.homeTeamImgUrl = homeTeamImgUrl;
    }

    public void setAwayTeamImgUrl(String awayTeamImgUrl) {
        this.awayTeamImgUrl = awayTeamImgUrl;
    }
}
