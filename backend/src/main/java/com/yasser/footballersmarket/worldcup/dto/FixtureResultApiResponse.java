package com.yasser.footballersmarket.worldcup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// minimal projection of api-football GET /fixtures?id= : final result + per-player match ratings.
// only the fields phase 4 consumes are mapped; everything else in the (large) payload is ignored.
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixtureResultApiResponse(List<FixtureResult> response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FixtureResult(Fixture fixture, League league, Teams teams, Goals goals, List<TeamPlayers> players) {}

    // id/date are used when fetching a date range to (re)build fixture rows (e.g. knockouts not seeded yet)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fixture(Long id, String date, Status status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record League(String round) {}

    // short = the api status code: NS/1H/HT/2H/FT/AET/PEN/PST...; FT/AET/PEN mean the match is done
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(@JsonProperty("short") String shortCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Teams(Team home, Team away) {}

    // winner = true for the side that advanced (handles knockouts decided on penalties); null/false otherwise
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Team(Long id, Boolean winner) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Goals(Integer home, Integer away) {}

    // one element per team, each carrying that team's player rows
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamPlayers(List<PlayerEntry> players) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerEntry(PlayerRef player, List<Statistics> statistics) {}

    // id is the rapid player id, canonical across our system (= player_world_cup_stats.player_id)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerRef(Long id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statistics(Games games, StatGoals goals) {}

    // rating is a string ("7.2") and null for unused subs (no minutes) -> not a played match
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Games(String rating) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatGoals(Integer total, Integer assists) {}
}
