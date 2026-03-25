package com.yasser.footballersmarket.scores365.dto;

import java.time.LocalDate;

public class PlayerRecentMatch {
    private String opponentTeamName;
    private Double rating;
    private String opponentTeamPhotoUrl;
    private Integer opponentTeamId;
    private LocalDate startDate;

    public PlayerRecentMatch(){}

    public PlayerRecentMatch(String opponentTeamName, Double rating, Integer opponentTeamId, LocalDate startDate) {
        this.opponentTeamName = opponentTeamName;
        this.rating = rating;
        this.opponentTeamId = opponentTeamId;
        this.startDate = startDate;
    }

    public String getOpponentTeamName() {
        return opponentTeamName;
    }

    public Double getRating() {
        return rating;
    }

    public String getOpponentTeamPhotoUrl() {
        return opponentTeamPhotoUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public Integer getOpponentTeamId() {
        return opponentTeamId;
    }

    public void setOpponentTeamPhotoUrl(String opponentTeamPhotoUrl) {
        this.opponentTeamPhotoUrl = opponentTeamPhotoUrl;
    }
}
