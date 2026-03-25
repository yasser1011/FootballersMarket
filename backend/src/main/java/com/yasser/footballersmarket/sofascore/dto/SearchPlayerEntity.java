package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchPlayerEntity {
    // player, manager, etc..
    private String type;
    private Integer id;
    private String name;
    private String teamName;
    private Integer teamId;
    private String sport;
    private String currentLeagueCountry;
    private String img;
    private String teamImg;

    public SearchPlayerEntity(){}

    public SearchPlayerEntity(Integer id, String name, String teamName, Integer teamId, String img, String teamImg) {
        this.id = id;
        this.name = name;
        this.teamName = teamName;
        this.teamId = teamId;
        this.img = img;
        this.teamImg = teamImg;
    }

    @JsonProperty("entity")
    private void getPlayerName(Map<String,Object> entity) {
        if (entity == null) {
            return; // Skip if entity is null
        }
//        this.name = (String)entity.get("name");
        this.name = safeGetString(entity, "name");
//        this.id = (Integer)entity.get("id");
        this.id = safeGetInteger(entity, "id");
        this.img = "/player/"+id+"/image";
        Map<String,Object> team = safeGetMap(entity, "team");
//        Map<String,Object> team = (Map<String,Object>)entity.get("team");
        // to avoid null exception as the manager entity has no team
        if (team != null){
            this.teamName = safeGetString(team, "name");
            this.teamId = safeGetInteger(team, "id");
            this.teamImg = "/team/" + teamId + "/image";
            Map<String,Object> sport = safeGetMap(team, "sport");
            this.sport = safeGetString(sport, "name");
            Map<String,Object> country = safeGetMap(team, "country");
            this.currentLeagueCountry = safeGetString(country, "name");
        }
    }




    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getTeamName() {
        return teamName;
    }

    public Integer getId() {
        return id;
    }

    public String getSport() {
        return sport;
    }

    public String getCurrentLeagueCountry() {
        return currentLeagueCountry;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public String getImg() {
        return img;
    }

    public String getTeamImg() {
        return teamImg;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public void setTeamImg(String teamImg) {
        this.teamImg = teamImg;
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> safeGetMap(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, T>) value : null;
    }

    private String safeGetString(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer safeGetInteger(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value instanceof Integer) return (Integer) value;
        return null;
    }
}
