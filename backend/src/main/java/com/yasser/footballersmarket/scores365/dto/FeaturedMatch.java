package com.yasser.footballersmarket.scores365.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasser.footballersmarket.scores365.ScoresConfig;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yasser.footballersmarket.scores365.util.JsonParser.*;

public class FeaturedMatch {
    private String homeTeamName;
    private String homeTeamId;
    private String homeTeamImgUrl;
    private String awayTeamName;
    private String awayTeamId;
    private String awayTeamImgUrl;
    private String status;
    private LocalDateTime startDatetime;

    public FeaturedMatch(){}

    public FeaturedMatch(String homeTeamName, String homeTeamId, String awayTeamName,
                         String awayTeamId, String status, LocalDateTime startDatetime
                         ) {
        this.homeTeamName = homeTeamName;
        this.homeTeamId = homeTeamId;
        this.awayTeamName = awayTeamName;
        this.awayTeamId = awayTeamId;
        this.status = status;
        this.startDatetime = startDatetime;
    }

    @JsonProperty("games")
    private void getMatchEventDetails(List<Map<String,Object>> games) {
        Map<String,Object> featuredGameObj = games.get(0);
        this.status = safeGetString(featuredGameObj, "statusText");
        Map<String, Object> homeCompetitor = safeGetMap(featuredGameObj, "homeCompetitor");
        Map<String, Object> awayCompetitor = safeGetMap(featuredGameObj, "awayCompetitor");
        this.homeTeamName = safeGetString(homeCompetitor, "name");
        this.homeTeamId = safeGetString(homeCompetitor, "id");
        this.awayTeamName = safeGetString(awayCompetitor, "name");
        this.awayTeamId = safeGetString(awayCompetitor, "id");
        String startDateTimeStr = safeGetString(featuredGameObj, "startTime");
        this.startDatetime = LocalDateTime.parse(startDateTimeStr.split("\\+")[0]);
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public String getHomeTeamId() {
        return homeTeamId;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public String getAwayTeamId() {
        return awayTeamId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }

    public String getHomeTeamImgUrl() {
        return homeTeamImgUrl;
    }

    public String getAwayTeamImgUrl() {
        return awayTeamImgUrl;
    }

    public void setHomeTeamImgUrl(String homeTeamImgUrl) {
        this.homeTeamImgUrl = homeTeamImgUrl;
    }

    public void setAwayTeamImgUrl(String awayTeamImgUrl) {
        this.awayTeamImgUrl = awayTeamImgUrl;
    }
}
