package com.yasser.footballersmarket.scores365.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yasser.footballersmarket.scores365.util.JsonParser.*;

public class PlayerDetailsDto {
    private String name;
    private String position;
    private String clubName;
    private Integer clubId;
    private String nationality;
    private LocalDate dateOfBirth;
    private PlayerLeagueStats leagueStats;
    private List<PlayerRecentMatch> recentMatches;

    private final Double DEFAULT_RATING = 6.5;

    public PlayerDetailsDto() {
    }

    @JsonProperty("athletes")
    private void getPlayerDetails(List<Map<String,Object>> athletes) {
        if (athletes == null || athletes.size() < 1) {
            return;
        }
        Map<String,Object> player = athletes.get(0);

        Map<String, Object> positionObj = safeGetMap(player, "position");
        this.position = safeGetString(positionObj, "name");
        this.name = safeGetString(player, "name");
        this.clubId = safeGetInteger(player, "clubId");
        this.nationality = safeGetString(player, "nationalityName");
        List<Map<String, Object>> playerDetListObj = safeGetListMap(player, "playerDetails");
        Map<String, Object> playerDetObj = playerDetListObj.get(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        this.dateOfBirth = LocalDate.parse(safeGetString(playerDetObj, "title"), formatter);

        List<Map<String, Object>> statList = safeGetListMap(player, "highlightStats");
        if(statList == null){
            this.leagueStats = null;
            this.recentMatches = new ArrayList<>();
            return;
        }

        Map<String, Object> leagueStatsObj = statList.get(0);

        List<Map<String, Object>> stats = safeGetListMap(leagueStatsObj, "stats");


        this.leagueStats = parsePlayerStatValues(stats);
        Map<String, Object> lastMatches = safeGetMap(player, "lastMatches");
        if (lastMatches != null)
            this.recentMatches = parsePlayerRecentMatchesRating(safeGetListMap(lastMatches, "games"));
    }
    @JsonProperty("competitors")
    private void getPlayerCurrentClub(List<Map<String,Object>> competitors){
        if (competitors == null || competitors.size() < 1) {
            this.clubName = "";
            return;
        }
        Map<String,Object> currentClubObj = competitors.get(0);
        this.clubName = safeGetString(currentClubObj, "name");
    }

    private PlayerLeagueStats parsePlayerStatValues(List<Map<String, Object>> stats) {
        int appearances = 0;
        int goals = 0;
        int assists = 0;
        double rating = DEFAULT_RATING;

        Pattern pattern = Pattern.compile("\\d+");

        for (Map<String, Object> statObj : stats) {
            String statName = safeGetString(statObj, "name");
            if (statName == null) continue;
            String value = safeGetString(statObj, "value");
            if ("rating".equalsIgnoreCase(statName)) {
                try {
                    if (value != null) {
                        rating = Double.parseDouble(value);
                    }
                } catch (NumberFormatException ignored) {
                }

            } else if ("appearances".equalsIgnoreCase(statName)) {
                if (value != null && !value.isEmpty()) {
                    try {
                        appearances = Integer.parseInt(value.split("/")[0]);
                    } catch (NumberFormatException ignored) {
                    }
                }

            } else if ("goals".equalsIgnoreCase(statName)) {
                if (value != null) {
                    Matcher m = pattern.matcher(value);
                    if (m.find()) goals = Integer.parseInt(m.group());
                }

            } else if ("assists".equalsIgnoreCase(statName)) {
                if (value != null) {
                    Matcher m = pattern.matcher(value);
                    if (m.find()) assists = Integer.parseInt(m.group());
                }
            }
        }

        return new PlayerLeagueStats(appearances, goals, assists, rating);
    }

    private Double parsePlayerRatingValue(List<Map<String,Object>> stats){
        for(Map<String,Object> statObj: stats){
            String statName = safeGetString(statObj, "name");
            if(statName == null || !statName.equalsIgnoreCase("rating"))
                continue;
            String rating = safeGetString(statObj, "value");
            try {
                return Double.parseDouble(rating);
            } catch (NumberFormatException e) {
                return DEFAULT_RATING;
            }
        }
        return DEFAULT_RATING;
    }

    private List<PlayerRecentMatch> parsePlayerRecentMatchesRating(List<Map<String,Object>> games){
        ArrayList<PlayerRecentMatch> playerRecentMatches = new ArrayList<>();
        if (games == null) {
            return playerRecentMatches;
        }

        for (Map<String, Object> match : games) {
            Boolean isPlayed = (Boolean) match.get("played");
            Boolean hasStats = (Boolean) match.get("hasStats");

            if(!isPlayed || !hasStats) continue;

            Map<String, Object> game = safeGetMap(match, "game");
            String startTime = safeGetString(game, "startTime");
            LocalDate startDate = null;
            if(startTime != null)
                startDate = LocalDate.parse(startTime.split("T")[0]);

            Integer playerTeamId = safeGetInteger(match, "relatedCompetitor");
            Map<String, Object> homeCompetitor = safeGetMap(game, "homeCompetitor");
            Integer homeCompetitorId = safeGetInteger(homeCompetitor, "id");
            Map<String, Object> awayCompetitor = safeGetMap(game, "awayCompetitor");
            Integer awayCompetitorId = safeGetInteger(awayCompetitor, "id");
            String opponentName = "";
            Integer opponentId;
//            String opponentPhotoUrl = "https://imagecache.365scores.com/image/upload/v5/Competitors/" + opponentId;
            if(!playerTeamId.equals(homeCompetitorId)){
                opponentName = safeGetString(homeCompetitor, "name");
                opponentId = safeGetInteger(homeCompetitor, "id");
            }else{
                opponentName = safeGetString(awayCompetitor, "name");
                opponentId = safeGetInteger(awayCompetitor, "id");
            }

            List<Map<String, Object>> statsListObj = safeGetListMap(match, "athleteStats");
            Map<String, Object> minutesPlayedObj = statsListObj.get(0);
            if (minutesPlayedObj == null || Integer.parseInt(safeGetString(minutesPlayedObj, "value")) <= 10) continue;
            Map<String, Object> ratingObj = statsListObj.get(4);
            Double rating = DEFAULT_RATING;
            if (ratingObj != null)
                rating = Double.valueOf(safeGetString(ratingObj, "value"));
            playerRecentMatches.add(new PlayerRecentMatch(opponentName, rating, opponentId, startDate));
        }

        return playerRecentMatches;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getClubName() {
        return clubName;
    }

    public String getNationality() {
        return nationality;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public PlayerLeagueStats getLeagueStats() {
        return leagueStats;
    }

    public Integer getClubId() {
        return clubId;
    }

    public List<PlayerRecentMatch> getRecentMatches() {
        return recentMatches;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLeagueStats(PlayerLeagueStats leagueStats) {
        this.leagueStats = leagueStats;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
