package com.yasser.footballersmarket.scores365.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yasser.footballersmarket.scores365.util.JsonParser.*;

public class FeaturedPlayersResponse {
    private List<FeaturedPlayer> featuredPlayers = new ArrayList<>();

    public FeaturedPlayersResponse() {
    }

    public FeaturedPlayersResponse(List<FeaturedPlayer> featuredPlayers) {
        this.featuredPlayers = featuredPlayers;
    }

    @JsonProperty("stats")
    private void fetchFeaturedPlayers(Map<String,Object> topPlayersResponse) {
        List<Map<String, Object>> playersStats = safeGetListMap(topPlayersResponse, "athletesStats");
//        Map<String,Object> playerRatingsListObj = playersStats.get(4);
        Map<String,Object> playerRatingsListObj = getStatsFilteredObjByCriteria("365 Ratings", playersStats);
        List<Map<String, Object>> playersList = safeGetListMap(playerRatingsListObj, "rows");

        int listProcessingLimit = Math.min(playersList.size(), 5);


        for (int i = 0; i < listProcessingLimit; i++){
            Map<String,Object> playerObj = playersList.get(i);
            Map<String, Object> playerDetObj = safeGetMap(playerObj, "entity");
            String playerName = safeGetString(playerDetObj, "name");
            Integer playerId = safeGetInteger(playerDetObj, "id");
            Integer playerTeamId = safeGetInteger(playerDetObj, "competitorId");

            List<Map<String, Object>> playerRatingStatList = safeGetListMap(playerObj, "stats");
            Map<String, Object> playerRatingStatObj = playerRatingStatList.get(0);
            String rating = safeGetString(playerRatingStatObj, "value");
            FeaturedPlayer featuredPlayer = new FeaturedPlayer(playerName, playerId, rating, i + 1, playerTeamId, "", "");

            featuredPlayers.add(featuredPlayer);
        }
    }

    public List<FeaturedPlayer> getFeaturedPlayers() {
        return featuredPlayers;
    }

    private Map<String,Object> getStatsFilteredObjByCriteria(String criteria, List<Map<String, Object>> playersStatsObjList){

        for (Map<String, Object> statsObj : playersStatsObjList) {
            String objCriteriaName = safeGetString(statsObj, "name");
            if(criteria.equalsIgnoreCase(objCriteriaName)){
                return statsObj;
            }
        }
        return new HashMap<>();
    }

    public void setFeaturedPlayers(List<FeaturedPlayer> featuredPlayers) {
        this.featuredPlayers = featuredPlayers;
    }
}
