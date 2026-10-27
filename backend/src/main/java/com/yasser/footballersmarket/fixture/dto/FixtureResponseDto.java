package com.yasser.footballersmarket.fixture.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FixtureResponseDto(
        Response[] response
) {
    public record Response(
            Fixture fixture,
            League league,
            Teams teams,
            Goals goals
    ){
        public record Fixture(
                String date,
                Long timestamp,
                Status status
        ){
            public record Status(
                    @JsonProperty("short") String shortName
            ){}
        }
        public record League(
            Long id,
            String name,
            String logo,
            String round
        ){

        }
        public record Teams(
                Home home,
                Away away
        ){
            public record Home(
                    Long id,
                    String name,
                    String logo,
                    Boolean winner
            ){}
            public record Away(
                    Long id,
                    String name,
                    String logo,
                    Boolean winner
            ){}
        }
        public record Goals(
                Integer home,
                Integer away
        ){

        }
    }
}



