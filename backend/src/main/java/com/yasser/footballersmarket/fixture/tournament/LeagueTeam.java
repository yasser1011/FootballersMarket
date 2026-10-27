package com.yasser.footballersmarket.fixture.tournament;

public record LeagueTeam(
        Long id,
        Team[] team
){
    public record Team(
            Long id
    ){}
}
