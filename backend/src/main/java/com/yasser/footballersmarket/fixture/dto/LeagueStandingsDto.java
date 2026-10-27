package com.yasser.footballersmarket.fixture.dto;

public record LeagueStandingsDto(
        Response[] response
) {
    public record Response(
            League league
    ){
        public record League(
                Long id,
                String name,
                String logo,
                Integer season,
                Standing[][] standings
        ){
            public record Standing(
                    Integer rank,
                    Team team
            ){
                public record Team(
                        Long id,
                        String name
                ){}
            }
        }
    }
}
