package com.yasser.footballersmarket.playerstats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yasser.footballersmarket.player.Player;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
public class PlayerWorldCupStats {
    @Id
    @Column(name = "player_id")
    private Long playerId;
    // rapid api world cup team id (world_cup_team.id)
    private Long teamId;
    private Integer goals;
    private Integer assists;
    private Integer totalNumOfGames;
    private Double rating;
    @OneToOne
    // to have same primary key as player primary key
    @MapsId
    // foreign key is defined here
    // insertion is from child here
    @JoinColumn(name = "player_id")
    // db-level cascade: deleting a player must never be blocked by his world cup stats
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Player player;

    public PlayerWorldCupStats(){}

    public PlayerWorldCupStats(Long teamId, Integer goals, Integer assists, Integer totalNumOfGames, Double rating, Player player) {
        this.teamId = teamId;
        this.goals = goals;
        this.assists = assists;
        this.totalNumOfGames = totalNumOfGames;
        this.rating = rating;
        this.player = player;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getTeamId() {
        return teamId;
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

    public Integer getTotalNumOfGames() {
        return totalNumOfGames;
    }

    public void setTotalNumOfGames(Integer totalNumOfGames) {
        this.totalNumOfGames = totalNumOfGames;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
