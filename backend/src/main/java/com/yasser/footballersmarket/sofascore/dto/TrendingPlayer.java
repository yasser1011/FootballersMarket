package com.yasser.footballersmarket.sofascore.dto;

public class TrendingPlayer {
    private String playerName;
    private Integer playerId;
    private String playerImgUrl;
    private String homeTeam;
    private String awayTeam;
    private String rating;
    private Integer playerTeamId;
    private String playerTeamImgUrl;

    public TrendingPlayer(){}

    public TrendingPlayer(String playerName, Integer playerId, String homeTeam, String awayTeam, String rating, Integer playerTeamId) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.rating = rating;
        this.playerTeamId = playerTeamId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public String getRating() {
        return rating;
    }

    public String getPlayerTeamImgUrl() {
        return playerTeamImgUrl;
    }

    public String getPlayerImgUrl() {
        return playerImgUrl;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getPlayerTeamId() {
        return playerTeamId;
    }

    public void setPlayerImgUrl(String playerImgUrl) {
        this.playerImgUrl = playerImgUrl;
    }

    public void setPlayerTeamImgUrl(String playerTeamImgUrl) {
        this.playerTeamImgUrl = playerTeamImgUrl;
    }
}
