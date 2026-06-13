package com.yasser.footballersmarket.featuredplayer;

import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
// uniqueness includes fixture_name so a world cup round's 5 rows don't collide with the daily club 5
// (which share dates/positions). club rows use fixture_name "" (non-null sentinel, NOT null) so the
// old per-day (date, position) uniqueness still holds for them — postgres treats nulls as distinct.
@Table(name = "featured_player", uniqueConstraints = {
        @UniqueConstraint(name = "uc_date_position_fixture", columnNames = {"date", "position", "fixture_name"})
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
    // world cup featured rows = per-round running top 5; club rows keep these defaults
    private Boolean worldCup = false;
    // the round (e.g. "Group Stage - 1") for WC rows; "" for club rows (keeps the unique sentinel).
    // column name pinned so it matches the fixture_name referenced in the @UniqueConstraint above
    @Column(name = "fixture_name")
    private String fixtureName = "";

    public FeaturedPlayer(){}

    // world cup featured row: playerId = internal rapid id, playerTeamId = WC team rapid id,
    // date = the match where this rating was earned, fixtureName = the round
    public static FeaturedPlayer worldCupRow(String playerName, Integer playerId, String rating,
                                             Integer playerTeamId, LocalDate date, Integer position, String round) {
        FeaturedPlayer fp = new FeaturedPlayer();
        fp.playerName = playerName;
        fp.playerId = playerId;
        fp.rating = rating;
        fp.playerTeamId = playerTeamId;
        fp.date = date;
        fp.position = position;
        fp.fixtureName = round;
        fp.worldCup = true;
        return fp;
    }

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

    public Integer getPosition() {
        return position;
    }

    public Boolean getWorldCup() {
        return worldCup;
    }

    public String getFixtureName() {
        return fixtureName;
    }
}