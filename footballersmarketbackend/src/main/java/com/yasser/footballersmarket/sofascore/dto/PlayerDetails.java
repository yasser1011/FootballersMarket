package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasser.footballersmarket.player.Player;

import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlayerDetails {
    @JsonProperty("name")
    private String name;

    @JsonProperty("position")
    private String position;
    private String team;
    private Integer teamId;
    private String leagueName;
    private String country;
    private LocalDate dateOfBirth;

    @JsonProperty("dateOfBirthTimestamp")
    private void getPlayerDateOfBirth(Integer dateOfBirthTimeStamp) {
        long l = TimeUnit.SECONDS.toMillis(dateOfBirthTimeStamp);
//        Timestamp tms = new Timestamp(l);
        Instant instant = Instant.ofEpochMilli(l);
//        LocalDateTime localDateTime =
//                LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        this.dateOfBirth = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate();
    }

    private Integer leagueSofascoreId;

    @JsonProperty("proposedMarketValue")
    private Integer price;

    public PlayerDetails(){}

    @JsonProperty("team")
    private void getPlayerData(Map<String,Object> team) {
        this.team = (String)team.get("name");
        this.teamId = (Integer) team.get("id");
        Map<String,Object> league = (Map<String,Object>)team.get("primaryUniqueTournament");
        if (league != null){
            this.leagueName = (String)league.get("name");
            this.leagueSofascoreId = (Integer)league.get("id");
        }
    }

    @JsonProperty("country")
    private void getPlayerCountryData(Map<String,Object> country) {
        this.country = (String)country.get("name");

    }

    public String getName() {
        return name;
    }

    public String getTeam() {
        return team;
    }
    public Integer getTeamId() {
        return teamId;
    }
    public Integer getPrice() {
        return price;
    }

    public String getLeagueName() {
        return leagueName;
    }
    public String getCountryName() {
        return country;
    }
    public String getPosition() {
        return position;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public Integer getLeagueSofascoreId() {
        return leagueSofascoreId;
    }
}
