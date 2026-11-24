package com.yasser.footballersmarket.playerstats;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

// one player's line in one fixture he played (league or champions league), written by the stats
// handler in the same transaction as his totals. only each player's 5 newest are kept: they are his
// "last 5" on the details page, and the latest round's rows are where the featured players come from.
// unused subs get no row, and only fixture-maintained players are ever written here
@Entity
@Table(name = "player_match_rating",
        indexes = @Index(name = "idx_player_match_rating_player_date", columnList = "player_id, fixture_date"))
@IdClass(PlayerMatchRating.Key.class)
public class PlayerMatchRating {
    @Id
    @Column(name = "player_id")
    private Long playerId;
    @Id
    @Column(name = "fixture_id")
    private Long fixtureId;

    // copied from the fixture, so "his newest 5" needs no join
    @Column(name = "fixture_date")
    private Instant fixtureDate;
    private Long leagueId;
    // the club he played for in that match
    private Long teamId;
    // -> the Team row for the opponent's name; null when the api did not say which side he was on
    private Long opponentTeamId;
    private Integer minutes;
    // the rating counted into his totals, the 6.5 default included
    private Double rating;
    private Integer goals;
    private Integer assists;

    public PlayerMatchRating() {}

    public PlayerMatchRating(Long playerId, Long fixtureId, Instant fixtureDate, Long leagueId, Long teamId,
                             Long opponentTeamId, Integer minutes, Double rating, Integer goals, Integer assists) {
        this.playerId = playerId;
        this.fixtureId = fixtureId;
        this.fixtureDate = fixtureDate;
        this.leagueId = leagueId;
        this.teamId = teamId;
        this.opponentTeamId = opponentTeamId;
        this.minutes = minutes;
        this.rating = rating;
        this.goals = goals;
        this.assists = assists;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public Instant getFixtureDate() {
        return fixtureDate;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Long getOpponentTeamId() {
        return opponentTeamId;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public Double getRating() {
        return rating;
    }

    public Integer getGoals() {
        return goals;
    }

    public Integer getAssists() {
        return assists;
    }

    // composite key: a player has at most one line per fixture
    public static class Key implements Serializable {
        private Long playerId;
        private Long fixtureId;

        public Key() {}

        public Key(Long playerId, Long fixtureId) {
            this.playerId = playerId;
            this.fixtureId = fixtureId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(playerId, key.playerId) && Objects.equals(fixtureId, key.fixtureId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerId, fixtureId);
        }
    }
}
