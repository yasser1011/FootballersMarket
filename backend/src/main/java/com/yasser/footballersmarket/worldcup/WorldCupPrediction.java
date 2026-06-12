package com.yasser.footballersmarket.worldcup;

import javax.persistence.*;

// one user's prediction for one fixture. editable until kickoff, settled once when the
// fixture finishes (awardedPoints set, settled flipped) so the poller can't double-pay
@Entity
@Table(name = "world_cup_prediction", uniqueConstraints = {
        @UniqueConstraint(name = "uc_user_fixture", columnNames = {"user_id", "fixture_id"})
})
public class WorldCupPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // explicit column names so the unique constraint above resolves regardless of naming strategy
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "fixture_id")
    private Long fixtureId;
    // null = predicted a draw; otherwise the team predicted to win
    private Long predictedWinnerTeamId;
    private Integer predictedHomeGoals;
    private Integer predictedAwayGoals;
    // computed at settlement (formula + bonusPoints); null until then
    private Integer awardedPoints;
    // manual lever: extra points added on top at settlement, default 0
    private Integer bonusPoints = 0;
    private boolean settled = false;

    public WorldCupPrediction() {}

    public WorldCupPrediction(Long userId, Long fixtureId, Long predictedWinnerTeamId,
                              Integer predictedHomeGoals, Integer predictedAwayGoals) {
        this.userId = userId;
        this.fixtureId = fixtureId;
        this.predictedWinnerTeamId = predictedWinnerTeamId;
        this.predictedHomeGoals = predictedHomeGoals;
        this.predictedAwayGoals = predictedAwayGoals;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public Long getPredictedWinnerTeamId() {
        return predictedWinnerTeamId;
    }

    public void setPredictedWinnerTeamId(Long predictedWinnerTeamId) {
        this.predictedWinnerTeamId = predictedWinnerTeamId;
    }

    public Integer getPredictedHomeGoals() {
        return predictedHomeGoals;
    }

    public void setPredictedHomeGoals(Integer predictedHomeGoals) {
        this.predictedHomeGoals = predictedHomeGoals;
    }

    public Integer getPredictedAwayGoals() {
        return predictedAwayGoals;
    }

    public void setPredictedAwayGoals(Integer predictedAwayGoals) {
        this.predictedAwayGoals = predictedAwayGoals;
    }

    public Integer getAwardedPoints() {
        return awardedPoints;
    }

    public void setAwardedPoints(Integer awardedPoints) {
        this.awardedPoints = awardedPoints;
    }

    public Integer getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(Integer bonusPoints) {
        this.bonusPoints = bonusPoints;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}
