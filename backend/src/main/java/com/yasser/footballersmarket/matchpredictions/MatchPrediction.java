package com.yasser.footballersmarket.matchpredictions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

// one user's prediction for one fixture, editable until kickoff.
// the unique constraint is what actually enforces "one prediction per user per fixture": the upsert
// is a find-then-save, so without it two concurrent requests could both insert
@Entity
@Table(name = "match_prediction", uniqueConstraints = {
        @UniqueConstraint(name = "uc_user_fixture", columnNames = {"user_id", "fixture_id"})
})
public class MatchPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // explicit column names so the unique constraint above resolves regardless of naming strategy
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "fixture_id")
    private Long fixtureId;
    private Integer homeScore;
    private Integer awayScore;
    // null = predicted a draw; otherwise the team predicted to win
    private Long winnerTeamId;
    // nominal value of this prediction once settled: formula points plus the fixture's bonus, and
    // negative on a miss. null (not 0) until settlement, so "unsettled" stays distinguishable from
    // "settled and earned nothing". the wallet may have floored at zero; this records the real value
    private Integer awardedPoints;
    // the idempotence latch. settlement reads only unsettled rows, so a rerun cannot pay twice --
    // independent of the fixture-level latch, which would not survive a manual rerun
    private boolean settled = false;

    public MatchPrediction(){}

    public MatchPrediction(Long fixtureId, Long userId, Integer homeScore, Integer awayScore, Long winnerTeamId){
        this.fixtureId = fixtureId;
        this.userId = userId;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.winnerTeamId = winnerTeamId;
    }

    public Long getId() {
        return id;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public Long getWinnerTeamId() {
        return winnerTeamId;
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public void setWinnerTeamId(Long winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public Integer getAwardedPoints() {
        return awardedPoints;
    }

    public void setAwardedPoints(Integer awardedPoints) {
        this.awardedPoints = awardedPoints;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}
