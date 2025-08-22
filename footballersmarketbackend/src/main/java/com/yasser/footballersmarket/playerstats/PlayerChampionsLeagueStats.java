package com.yasser.footballersmarket.playerstats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yasser.footballersmarket.player.Player;

import javax.persistence.*;

@Entity
public class PlayerChampionsLeagueStats {
    @Id
    @Column(name = "player_id")
    private Long playerId;
    private Integer totalNumOfGames;
    private Integer goals;
    private Integer assists;
    private Double rating;
    @OneToOne
    // to have same primary key as player primary key
    @MapsId
    // foreign key is defined here
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;

    public PlayerChampionsLeagueStats(){}

    public PlayerChampionsLeagueStats(Integer goals, Integer assists, Double rating) {
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
    }

    public PlayerChampionsLeagueStats(Integer totalNumOfGames, Integer goals, Integer assists, Double rating) {
        this.totalNumOfGames = totalNumOfGames;
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
    }

    public PlayerChampionsLeagueStats(Long playerId, Integer totalNumOfGames, Integer goals, Integer assists, Double rating, Player player) {
        this.playerId = playerId;
        this.totalNumOfGames = totalNumOfGames;
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
        this.player = player;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getTotalNumOfGames() {
        return totalNumOfGames;
    }

    public void setTotalNumOfGames(Integer totalNumOfGames) {
        this.totalNumOfGames = totalNumOfGames;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Integer getGoals() {
        return goals;
    }

    public void setGoals(Integer goals) {
        this.goals = goals;
    }

    public Integer getAssists() {
        return assists;
    }

    public void setAssists(Integer assists) {
        this.assists = assists;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
