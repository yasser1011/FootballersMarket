package com.yasser.footballersmarket.fixture;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

// a discovered club fixture. team references are plain rapid ids rather than associations, matching
// WorldCupFixture: a team is shared by many fixtures, so there is no ownership to cascade.
//
// INSERT-ONLY from discovery. the daily job re-reads today and tomorrow, so a fixture is seen more
// than once; rewriting it would let discovery wipe score/status that a results poller owns. once a
// row exists, discovery leaves it alone.
@Entity
@Table(name = "fixture")
public class Fixture {
    // rapid api fixture id, assigned from the api response (no generation)
    @Id
    private Long id;
    private Instant date;
    private Long leagueId;
    private String round;
    private String status;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeGoals;
    private Integer awayGoals;
    // the team the api flagged as winner; null = draw or not yet decided
    private Long winnerTeamId;
    // league table positions of the two sides AS AT DISCOVERY -- a snapshot, not a live lookup.
    // settlement scores against these, so the reward cannot change if settlement runs late or is
    // re-run, and insert-only discovery guarantees they are never rewritten.
    // null for teams with no league table we track (champions league sides from other leagues)
    private Integer homeTeamPosition;
    private Integer awayTeamPosition;
    // manual lever for a marquee match: extra points settlement adds on top of the formula for
    // every prediction on this fixture. set out of band (admin/sql), and insert-only discovery
    // never rewrites an existing row, so a bonus set here survives later discovery runs.
    // null on rows created before the column existed -- settlement must treat null as 0
    private Integer bonusPoints = 0;

    public Fixture() {}

    public Fixture(Long id, Instant date, Long leagueId, String round, String status,
                       Long homeTeamId, Long awayTeamId, Integer homeGoals, Integer awayGoals,
                       Long winnerTeamId) {
        this.id = id;
        this.date = date;
        this.leagueId = leagueId;
        this.round = round;
        this.status = status;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.winnerTeamId = winnerTeamId;
    }

    public Long getId() {
        return id;
    }

    public Instant getDate() {
        return date;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public String getRound() {
        return round;
    }

    public String getStatus() {
        return status;
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

    // written only by the results poll. discovery owns creation and never rewrites an existing row,
    // so these cannot be clobbered back to their kickoff values
    public void setStatus(String status) {
        this.status = status;
    }

    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }

    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }

    public void setWinnerTeamId(Long winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public Integer getHomeTeamPosition() {
        return homeTeamPosition;
    }

    public Integer getAwayTeamPosition() {
        return awayTeamPosition;
    }

    public void setHomeTeamPosition(Integer homeTeamPosition) {
        this.homeTeamPosition = homeTeamPosition;
    }

    public void setAwayTeamPosition(Integer awayTeamPosition) {
        this.awayTeamPosition = awayTeamPosition;
    }

    public Integer getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(Integer bonusPoints) {
        this.bonusPoints = bonusPoints;
    }
}
