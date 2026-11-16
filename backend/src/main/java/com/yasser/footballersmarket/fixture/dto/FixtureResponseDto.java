package com.yasser.footballersmarket.fixture.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

// projection of api-football GET /fixtures, used for both shapes of that call:
//   ?league=&from=&to=  -> discovery; returns fixtures without the players block (players stays null)
//   ?id= / ?ids=a-b-c   -> results; returns the same structure PLUS per-player minutes and ratings
// one record serves both because unknown/absent properties are ignored
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixtureResponseDto(
        // the api reports problems (bad ids, quota) with HTTP 200 and this field: [] when fine, an
        // object of messages when not. JsonNode because the shape varies; size() > 0 means an error
        JsonNode errors,
        Response[] response
) {
    // the shape most callers want: a response with no errors block
    public FixtureResponseDto(Response[] response) {
        this(null, response);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Fixture fixture,
            League league,
            Teams teams,
            Goals goals,
            // one element per team, each carrying that team's player rows. null on a range query
            List<TeamPlayers> players
    ){
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Fixture(
                Long id,
                String date,
                Long timestamp,
                Status status
        ){
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Status(
                    @JsonProperty("short") String shortName
            ){}
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record League(
            Long id,
            String name,
            String logo,
            String round
        ){
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Teams(
                Home home,
                Away away
        ){
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Home(
                    Long id,
                    String name,
                    String logo,
                    Boolean winner
            ){}
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Away(
                    Long id,
                    String name,
                    String logo,
                    Boolean winner
            ){}
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Goals(
                Integer home,
                Integer away
        ){
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TeamPlayers(TeamRef team, List<PlayerEntry> players) {}

        // the club this block of players belongs to (their club AT this match, which is how a
        // transfer to a club we do not track shows up)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TeamRef(Long id, String name) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PlayerEntry(PlayerRef player, List<Statistics> statistics) {}

        // id is the rapid player id, canonical across our system
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PlayerRef(Long id, String name) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Statistics(Games games, StatGoals goals) {}

        // minutes is 0 for an unused sub. rating is a string ("7.2"): "0" or null for unused subs, and
        // rarely null for a player who did play -> FixtureApiMapping decides what counts and what
        // rating to use
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Games(Integer minutes, String rating) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record StatGoals(Integer total, Integer assists) {}
    }
}
