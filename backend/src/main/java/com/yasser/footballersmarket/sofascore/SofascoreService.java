package com.yasser.footballersmarket.sofascore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.dto.*;
import me.xuender.unidecode.Unidecode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SofascoreService {

    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private final SofascoreConfig sofascoreConfig;

    Logger logger = LoggerFactory.getLogger(SofascoreService.class);

    public SofascoreService(WebClient webClient, SofascoreConfig sofascoreConfig, RestTemplate restTemplate) {
        this.webClient = webClient;
        this.sofascoreConfig = sofascoreConfig;
        this.restTemplate = restTemplate;
    }

    // get player id from name api
    // get league id from player id api
    // get current season from league id

    // for scheduler to get the closest player related to the name from rapid api scheduler when searching for player multiple leagues
    public SearchPlayerEntity searchClosestPlayerByName(String playerName){
        try {
            playerName = Unidecode.decode(playerName);

            String sofascoreApiBaseUrl = sofascoreConfig.getSofascoreApiBaseUrl();
            String url = sofascoreApiBaseUrl + "/search/all?q=" + playerName + "&page=0";
            logger.info("searching player name {} in sofascore search url {}", playerName, url);
            SearchEntityResponse res = restTemplate.getForObject(
                    url,
                    SearchEntityResponse.class);
            // check first character of player name in each
            // reason for this check is if you search T.Davies returns from api Ben Davies which are different players
            if (res == null || res.getResults().size() == 0){
                logger.error("player name {} not found in sofascore", playerName);
                return null;
            }
            List<SearchPlayerEntity> filteredList = res.getResults().stream()
                    .filter(searchPlayerEntity ->
                            searchPlayerEntity.getType().equals("player")
                                    && searchPlayerEntity.getSport().equals("Football"))
                    .peek(player -> {
                        player.setImg(sofascoreApiBaseUrl + player.getImg());
                        player.setTeamImg(sofascoreApiBaseUrl + player.getTeamImg());
                    })
                    .collect(Collectors.toList());



            for (SearchPlayerEntity player : filteredList) {
                String searchPlayerName = Unidecode.decode(player.getName());
                int paramNameLength = playerName.length();
                int resultPlayerSearchNameLength = searchPlayerName.length();
                System.out.println(searchPlayerName + " " + player.getType() + " " + player.getId() + " " + player.getTeamName() + " " + player.getSport());
                if(searchPlayerName.toLowerCase().charAt(0) == playerName.toLowerCase().charAt(0) &&
                        searchPlayerName.toLowerCase().charAt(resultPlayerSearchNameLength - 1) == playerName.toLowerCase().charAt(paramNameLength - 1) &&
                        searchPlayerName.toLowerCase().charAt(resultPlayerSearchNameLength - 2) == playerName.toLowerCase().charAt(paramNameLength - 2)){
                    return player;
                }
            }
            logger.info("player found in sofascore name {}", res.getResults().get(0).getName());
            return res.getResults().get(0);
        }catch (Exception e){
            logger.error("exception in sofascore search {}", e.getMessage());
            return null;
        }

    }

    // for when user searches for a player
    public List<SearchPlayerEntity> searchPlayersByName(String playerName){
        String sofascoreApiBaseUrl = sofascoreConfig.getSofascoreApiBaseUrl();
        String url = sofascoreApiBaseUrl + "/search/all?q=" + playerName + "&page=0";
        logger.info("searching in sofascore player name {} search url {}", playerName, url);

        SearchEntityResponse res = restTemplate.getForObject(
                url,
                SearchEntityResponse.class);
        if (res == null){
            logger.error("error getting search list from sofascore player name {}", playerName);
            throw new IllegalStateException("error getting search list from sofascore");
        }
        List<SearchPlayerEntity> filteredList = res.getResults().stream()
                .filter(searchPlayerEntity ->
                        searchPlayerEntity.getType().equals("player")
                                && searchPlayerEntity.getSport().equals("Football"))
                .peek(player -> {
                    player.setImg(sofascoreApiBaseUrl + player.getImg());
                    player.setTeamImg(sofascoreApiBaseUrl + player.getTeamImg());
                })
                .collect(Collectors.toList());

        logger.info("returned player list from sofascore search player name {}", playerName);
        return filteredList.subList(0, Math.min(5, filteredList.size()));
    }

    public PlayerDetails getPlayerGenericDetails(Integer playerId){
        String url = sofascoreConfig.getSofascoreApiBaseUrl() + "/player/" + playerId;
        logger.info("get player generic details from sofascore, sofascore id {} search url {}", playerId, url);

        PlayerDetailsResponse playerDetailsResponse = restTemplate.getForObject(
                url,
                PlayerDetailsResponse.class);
        PlayerDetails playerDetails = playerDetailsResponse.getPlayer();
        logger.info("got player, name {} club {}", playerDetails.getName(), playerDetails.getTeam());
        return playerDetails;
    }

    public String getLeagueCurrentSeasonId(Integer leagueId) throws JsonProcessingException {

        String leagueSeasonsResponse = restTemplate.getForObject(
                sofascoreConfig.getSofascoreApiBaseUrl() + "/unique-tournament/" +leagueId+ "/seasons",
                String.class);


        JsonNode seasonsList = new ObjectMapper().readTree(leagueSeasonsResponse).get("seasons");
        String leagueYearRes = seasonsList.get(0).get("year").asText();
        String currentConfigSeasonYear = sofascoreConfig.getCurrentSeasonYear();
        System.out.println(currentConfigSeasonYear);
        if (leagueYearRes.equals(currentConfigSeasonYear)){
            return seasonsList.get(0).get("id").asText();
        }else{
            // if im configuring the previous season number and still new season is not played
            return seasonsList.get(1).get("id").asText();
        }
    }

    public PlayerLeagueStatsDetails getPlayerStats(Integer playerId, Integer leagueId, String currentSeasonId) throws JsonProcessingException {
        try {

            String url = sofascoreConfig.getSofascoreApiBaseUrl() + "/player/" + playerId + "/unique-tournament/" + leagueId + "/season/" + currentSeasonId + "/statistics/overall";
            logger.info("getting player league stats from sofascore url {}", url);

            PlayerStatsResponse playerStatsResponse = restTemplate.getForObject(
                    url,
                    PlayerStatsResponse.class);

            PlayerLeagueStatsDetails playerStatsDetails = playerStatsResponse.getStatistics();

            return playerStatsDetails;
        }catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e){
            // player details not found for the current season in sofascore, return default data
            return new PlayerLeagueStatsDetails(6.5, 0, 0, 0);
        }

    }

    public PlayerLeagueStatsDetails getPlayerLeagueStats(Integer playerId) throws JsonProcessingException {
        PlayerDetails playerGenericDetails = getPlayerGenericDetails(playerId);
        if (playerGenericDetails == null) return null;

        Integer currentLeagueId = playerGenericDetails.getLeagueSofascoreId();
        String leagueCurrentSeasonId = getLeagueCurrentSeasonId(currentLeagueId);
        if (leagueCurrentSeasonId == null) return null;

        PlayerLeagueStatsDetails playerLeagueStats = getPlayerStats(playerId, currentLeagueId, leagueCurrentSeasonId);

        return playerLeagueStats;
    }

    public PlayerRating[] getPlayerRecentRatings(Integer playerId) throws JsonProcessingException {
        PlayerDetails playerGenericDetails = getPlayerGenericDetails(playerId);
        if (playerGenericDetails == null) return new PlayerRating[]{};

        Integer currentLeagueId = playerGenericDetails.getLeagueSofascoreId();
        String leagueCurrentSeasonId = getLeagueCurrentSeasonId(currentLeagueId);

        try {

            String url = sofascoreConfig.getSofascoreApiBaseUrl() + "/player/" + playerId + "/unique-tournament/" + currentLeagueId + "/season/" + leagueCurrentSeasonId + "/last-ratings";
            logger.info("getting player recent rating from sofascore url {}", url);
            String recentRatingResponse = restTemplate.getForObject(
                    url,
                    String.class);

            JsonNode recentRatings = new ObjectMapper().readTree(recentRatingResponse).get("lastRatings");
            PlayerRating[] playerRecentRatings = new ObjectMapper()
                    .treeToValue(recentRatings, PlayerRating[].class);

            return playerRecentRatings;
        }catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e){
            return new PlayerRating[]{};
        }

    }

    public Player getPlayerEntityFromSofascore(Integer playerId) throws JsonProcessingException {
        PlayerDetails playerGenericDetails = getPlayerGenericDetails(playerId);
        if (playerGenericDetails == null) return null;

        Integer currentLeagueId = playerGenericDetails.getLeagueSofascoreId();
        String leagueCurrentSeasonId = getLeagueCurrentSeasonId(currentLeagueId);
        if (leagueCurrentSeasonId == null) return null;

        PlayerLeagueStatsDetails playerLeagueStats = getPlayerStats(playerId, currentLeagueId, leagueCurrentSeasonId);

        Player player = new Player();
        player.setSofascoreId(playerId);
        player.setName(playerGenericDetails.getName());
        player.setNationality(playerGenericDetails.getCountryName());
        player.setDateOfBirth(playerGenericDetails.getDateOfBirth());
        player.setPosition(playerGenericDetails.getPosition());
        player.setRapidApiClub("");
        player.setSofascoreClub(playerGenericDetails.getTeam());
        // https://api.sofascore.com/api/v1/player/839956/image
        player.setPhotoUrl(sofascoreConfig.getSofascoreApiBaseUrl() + "/player/" + playerId + "/image");
        // https://api.sofascore.app/api/v1/team/17/image
        player.setClubPhotoUrl(sofascoreConfig.getSofascoreApiBaseUrl() + "/team/" + playerGenericDetails.getTeamId() + "/image");

        PlayerLeagueStats playerEntityLeagueStats = new PlayerLeagueStats();
        playerEntityLeagueStats.setTotalNumOfGames(playerLeagueStats.getCountRating());
        playerEntityLeagueStats.setGoals(playerLeagueStats.getGoals());
        playerEntityLeagueStats.setAssists(playerLeagueStats.getGoalsAssistsSum() - playerLeagueStats.getGoals());
        playerEntityLeagueStats.setRating(playerLeagueStats.getRating());

        player.setLeagueStats(playerEntityLeagueStats);

        return player;
    }


    public FeaturedMatch getFootballFeaturedMatch(){

        FeaturedMatch featuredFootballMatchEvent = restTemplate.getForObject(
                sofascoreConfig.getSofascoreEventBaseUrl() + "/odds/1/featured-events-by-tiers/football",
                FeaturedMatch.class);
        addBaseUrlToFeaturedMatchResponse(featuredFootballMatchEvent);
        return featuredFootballMatchEvent;
    }

    public FeaturedPlayersResponse getTrendingPlayers(){
        String url = sofascoreConfig.getSofascoreEventBaseUrl() + "/sport/football/trending-top-players";
        TrendingPlayersResponse trendingPlayersResponse = restTemplate.getForObject(
                url,
                TrendingPlayersResponse.class);
        return convertToFeaturedPlayersDto(trendingPlayersResponse);
    }

    private FeaturedPlayersResponse convertToFeaturedPlayersDto(TrendingPlayersResponse trendingPlayersResponse){
        if (trendingPlayersResponse == null) return null;

        List<FeaturedPlayer> featuredPlayersList = new ArrayList<>();

        for (TrendingPlayer player: trendingPlayersResponse.getTrendingPlayers()) {
            FeaturedPlayer featuredPlayer = new FeaturedPlayer(player.getPlayerName(), player.getPlayerId(),
                    player.getRating(), player.getPlayerTeamId(), player.getHomeTeam(), player.getAwayTeam());
            featuredPlayersList.add(featuredPlayer);
        }
        return new FeaturedPlayersResponse(featuredPlayersList);
    }


    private void addBaseUrlToFeaturedMatchResponse(FeaturedMatch featuredMatch){
        if (featuredMatch == null) return;
        featuredMatch.setHomeTeamImgUrl(sofascoreConfig.getSofascoreApiBaseUrl() + featuredMatch.getHomeTeamImgUrl());
        featuredMatch.setAwayTeamImgUrl(sofascoreConfig.getSofascoreApiBaseUrl() + featuredMatch.getAwayTeamImgUrl());

    }

    public void addBaseUrlToTrendingPlayersResponse(FeaturedPlayersResponse response){
        if (response == null) return;
        List<FeaturedPlayer> trendingPlayers = response.getFeaturedPlayers();
        trendingPlayers
                .forEach(player -> {
                    String sofascoreApiBaseUrl = sofascoreConfig.getSofascoreApiBaseUrl();
                    player.setPlayerImgUrl(sofascoreApiBaseUrl + "/player/"+ player.getPlayerId() +"/image");
                    player.setPlayerTeamImgUrl(sofascoreApiBaseUrl + "/team/"+ player.getPlayerTeamId() +"/image");
                });
        response.setFeaturedPlayers(trendingPlayers);
    }

}
