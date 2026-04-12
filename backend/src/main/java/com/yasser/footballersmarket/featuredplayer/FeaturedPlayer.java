package com.yasser.footballersmarket.featuredplayer;

import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "featured_player", uniqueConstraints = {
        @UniqueConstraint(name = "uc_date_position", columnNames = {"date", "position"})
})
public class FeaturedPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String playerName;
    private Integer playerId;
    private String rating;
    private Integer playerTeamId;
    @Transient
    private String playerImgUrl;
    @Transient
    private String playerTeamImgUrl;
    private LocalDate date;
    private Integer position;
    private String homeTeamName;
    private String awayTeamName;

    public FeaturedPlayer(){}

    public FeaturedPlayer(String playerName, Integer playerId, String rating, Integer position, Integer playerTeamId, String homeTeamName, String awayTeamName) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.rating = rating;
        this.position = position;
        this.playerTeamId = playerTeamId;
        this.date = LocalDate.now();
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
    }

    public FeaturedPlayer(String playerName, Integer playerId, String rating, Integer playerTeamId, LocalDate date, Integer position, String homeTeamName, String awayTeamName) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.rating = rating;
        this.playerTeamId = playerTeamId;
        this.date = date;
        this.position = position;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
    }


    public Long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public String getRating() {
        return rating;
    }

    public String getPlayerImgUrl() {
        return playerImgUrl;
    }

    public String getPlayerTeamImgUrl() {
        return playerTeamImgUrl;
    }

    public Integer getPlayerTeamId() {
        return playerTeamId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setPlayerImgUrl(String playerImgUrl) {
        this.playerImgUrl = playerImgUrl;
    }

    public void setPlayerTeamImgUrl(String playerTeamImgUrl) {
        this.playerTeamImgUrl = playerTeamImgUrl;
    }
}