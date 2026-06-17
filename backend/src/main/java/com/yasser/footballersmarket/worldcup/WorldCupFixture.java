package com.yasser.footballersmarket.worldcup;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "world_cup_fixture")
public class WorldCupFixture {
    // rapid api ids assigned from scrap data (no generation)
    @Id
    private Long id;
    private Instant date;
    private String round;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeGoals;
    private Integer awayGoals;
    // minutes played, from the api status while the match is in play (null otherwise); for the live UI
    private Integer elapsed;
    // the team that won / advanced (rapid 'winner' flag); null = draw. drives prediction settlement
    // and handles knockouts decided on penalties, where goals alone would read as a draw
    private Long winnerTeamId;
    private String status;
    // minutes after kickoff to poll the api for the final result;
    // increase(+30 or 60) each time the match is found still unfinished
    private Integer checkAgainMinutes = 120;

    public WorldCupFixture() {}

    public WorldCupFixture(Long id, Instant date, String round,
                           Long homeTeamId, Long awayTeamId, String status) {
        this.id = id;
        this.date = date;
        this.round = round;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Instant getDate() {
        return date;
    }

    public String getRound() {
        return round;
    }

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public Long getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(Long winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCheckAgainMinutes() {
        return checkAgainMinutes;
    }

    public void setCheckAgainMinutes(Integer checkAgainMinutes) {
        this.checkAgainMinutes = checkAgainMinutes;
    }

    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }

    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }

    public Integer getElapsed() {
        return elapsed;
    }

    public void setElapsed(Integer elapsed) {
        this.elapsed = elapsed;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
