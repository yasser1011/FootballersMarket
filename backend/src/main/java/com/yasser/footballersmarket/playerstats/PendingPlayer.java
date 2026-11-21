package com.yasser.footballersmarket.playerstats;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

// a player seen in a league fixture who is not in the player table yet: a new signing or an academy
// debut. the lineup only gives an id and a name, which is not enough to create a player (no birth
// date for the price, no external id for search/photos), so the stats handler just records him here
// and a later job fills in his details and creates him.
//
// keyed by the rapid player id, so seeing him in several fixtures before that job runs is one row
@Entity
@Table(name = "pending_player")
public class PendingPlayer {
    // rapid api player id, assigned from the lineup
    @Id
    private Long id;
    private String name;
    // the club he played for when first seen
    private Long teamId;
    private Long firstSeenFixtureId;
    private Instant firstSeenAt;

    public PendingPlayer() {}

    public PendingPlayer(Long id, String name, Long teamId, Long firstSeenFixtureId, Instant firstSeenAt) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
        this.firstSeenFixtureId = firstSeenFixtureId;
        this.firstSeenAt = firstSeenAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Long getFirstSeenFixtureId() {
        return firstSeenFixtureId;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }
}
