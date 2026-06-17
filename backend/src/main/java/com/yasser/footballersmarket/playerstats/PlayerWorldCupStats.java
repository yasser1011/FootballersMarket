package com.yasser.footballersmarket.playerstats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.worldcup.WorldCupTeam;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
public class PlayerWorldCupStats {
    @Id
    @Column(name = "player_id")
    private Long playerId;
    // rapid api world cup team id (world_cup_team.id). @Column matches the team relation's
    // join column logical name so both can share the physical team_id column
    @Column(name = "team_id")
    private Long teamId;
    // last season league rating snapshot, frozen at seed time;
    // base price is computed from it in code and must never drift mid-tournament
    private Double baseRating;
    private Integer goals;
    private Integer assists;
    private Integer totalNumOfGames;
    // world cup performance rating, starts at 6.5 and drives price movement
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

    // the player's national team, for the world cup view's nationality flag + name. read-only
    // (teamId is the writable column); @NotFound(IGNORE) keeps it FK-constraint-free and null
    // when no team row exists (e.g. tests), so it never blocks saving a stats row
    @ManyToOne
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private WorldCupTeam team;

    public PlayerWorldCupStats(){}

    public PlayerWorldCupStats(Long teamId, Double baseRating, Integer goals, Integer assists,
                               Integer totalNumOfGames, Double rating, Player player) {
        this.teamId = teamId;
        this.baseRating = baseRating;
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

    public Double getBaseRating() {
        return baseRating;
    }

    public void setBaseRating(Double baseRating) {
        this.baseRating = baseRating;
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

    public WorldCupTeam getTeam() {
        return team;
    }
}
