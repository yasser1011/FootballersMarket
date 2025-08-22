package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrendingPlayersResponse {

    private List<TrendingPlayer> trendingPlayers = new ArrayList<>();


    public TrendingPlayersResponse(){}

    @JsonProperty("topPlayers")
    private void fetchTrendingPlayers(List<Map<String,Object>> topPlayers) {
        int listProcessingLimit = Math.min(topPlayers.size(), 5);

        for (int i = 0; i < listProcessingLimit; i++){
            Map<String,Object> playerMatchObj = topPlayers.get(i);
            String playerRating = String.valueOf(playerMatchObj.get("rating"));

            Map<String,Object> playerObj = (Map<String,Object>)playerMatchObj.get("player");
            String playerName = (String) playerObj.get("name");
            Integer playerId = (Integer) playerObj.get("id");
            String playerImgUrl = "/player/"+ playerId +"/image";

            Map<String,Object> playerTeamObj = (Map<String,Object>)playerMatchObj.get("team");
            Integer playerTeamId = (Integer) playerTeamObj.get("id");
            String playerTeamImgUrl = "/team/"+ playerTeamId +"/image";

            Map<String,Object> eventObj = (Map<String,Object>)playerMatchObj.get("event");

            Map<String,Object> homeTeamObj = (Map<String,Object>)eventObj.get("homeTeam");
            Map<String,Object> awayTeamObj = (Map<String,Object>)eventObj.get("awayTeam");
            String homeTeamName = (String) homeTeamObj.get("name");
            String awayTeamName = (String) awayTeamObj.get("name");

            TrendingPlayer trendingPlayer = new TrendingPlayer(playerName, playerId, homeTeamName, awayTeamName, playerRating, playerTeamId);
            trendingPlayers.add(trendingPlayer);
        }
    }

    public List<TrendingPlayer> getTrendingPlayers() {
        return trendingPlayers;
    }

    public void setTrendingPlayers(List<TrendingPlayer> trendingPlayers) {
        this.trendingPlayers = trendingPlayers;
    }
}
