package com.yasser.footballersmarket.fixture.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// projection of api-football GET /fixtures, used for both shapes of that call:
//   ?league=&from=&to=  -> discovery; returns fixtures without the players block (players stays null)
//   ?id=                -> results; returns the same structure PLUS per-player match ratings
// one record serves both because unknown/absent properties are ignored
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixtureResponseDto(
        Response[] response
) {
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
        public record TeamPlayers(List<PlayerEntry> players) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PlayerEntry(PlayerRef player, List<Statistics> statistics) {}

        // id is the rapid player id, canonical across our system
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PlayerRef(Long id, String name) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Statistics(Games games, StatGoals goals) {}

        // rating is a string ("7.2") and null for unused subs (no minutes) -> did not play
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Games(String rating) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record StatGoals(Integer total, Integer assists) {}
    }
}
