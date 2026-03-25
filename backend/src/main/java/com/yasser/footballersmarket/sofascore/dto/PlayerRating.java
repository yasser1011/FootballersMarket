package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerRating {
    private String opponentTeamName;
    private Double rating;
    private Integer opponentTeamId;
    private LocalDate startDate;

    public PlayerRating(){}

    public PlayerRating(String opponentTeamName, Double rating, Integer opponentTeamId, LocalDate startDate) {
        this.opponentTeamName = opponentTeamName;
        this.rating = rating;
        this.opponentTeamId = opponentTeamId;
        this.startDate = startDate;
    }

    @JsonProperty("opponent")
    private void getLeagueDetails(Map<String,Object> opponent) {
        this.opponentTeamId = (Integer)opponent.get("id");
        this.opponentTeamName = (String)opponent.get("name");
    }

    @JsonProperty("startTimestamp")
    private void getGameStartDate(Integer startTimestamp) {
        long l = TimeUnit.SECONDS.toMillis(startTimestamp);
        Instant instant = Instant.ofEpochMilli(l);

        this.startDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
    }

    public String getOpponentTeamName() {
        return opponentTeamName;
    }

    public Double getRating() {
        return rating;
    }

    public Integer getOpponentTeamId() {
        return opponentTeamId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
