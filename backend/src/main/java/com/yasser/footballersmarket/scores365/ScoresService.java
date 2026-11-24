package com.yasser.footballersmarket.scores365;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerRecentMatchService;
import com.yasser.footballersmarket.scores365.dto.*;
import com.yasser.footballersmarket.sofascore.dto.PlayerLeagueStatsDetails;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats;
import me.xuender.unidecode.Unidecode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScoresService {
    private final RestTemplate restTemplate;
    // featured players only — see RestTemplateConfig for why that call needs its own instance
    private final RestTemplate shortTimeoutRestTemplate;
    private final ScoresConfig scoresConfig;
    private final PlayerService playerService;
    private final PlayerRecentMatchService recentMatchService;

    private final Logger logger = LoggerFactory.getLogger(ScoresService.class);

    public ScoresService(RestTemplate restTemplate,
                         @Qualifier("shortTimeoutRestTemplate") RestTemplate shortTimeoutRestTemplate,
                         ScoresConfig scoresConfig, PlayerService playerService,
                         PlayerRecentMatchService recentMatchService) {
        this.recentMatchService = recentMatchService;
        this.restTemplate = restTemplate;
        this.shortTimeoutRestTemplate = shortTimeoutRestTemplate;
        this.scoresConfig = scoresConfig;
        this.playerService = playerService;
    }
    // main method for fetching player details page
    public PlayerDetailsResDto getPlayerDetailsById(Integer playerId) {
        // fetch player stats from external service
        // check if player is available in db
        // if available return everything as is except league stats and recent games stats
        // if clubs are not matching
        // update in db from here
        PlayerDetailsResDto playerDetailsByExternalId = playerService.getPlayerDetailsByExternalId(playerId);
        PlayerDetailsDto playerDetailsResponseByExternalId = fetchPlayerEntityFromExternalService(playerId);
        // if any errors occur in the web service return db data
        if(playerDetailsResponseByExternalId == null){
            logger.error("player not found by external id {}", playerId);
            if(playerDetailsByExternalId == null){
                throw new EntityNotFoundException("player not found in both db and scores service external id " + playerId);
            }else{
                logger.error("player not found by external id {} returning db data", playerId);
                return playerDetailsByExternalId;
            }
        }
        logger.info("player found in external service id {}", playerId);

        // new player not found in db
        if(playerDetailsByExternalId == null){
            logger.info("new player not found in database external id {}", playerId);
            // converter already defaults missing league stats to 6.5 and prices accordingly
            return convertDtoToPlayerDetailsDto(playerId, playerDetailsResponseByExternalId, playerDetailsResponseByExternalId.getRecentMatches());
        }
        logger.info("player found in database external id {} internal id {}", playerId, playerDetailsByExternalId.getId());
        // a fixture-maintained player's last 5 come from our own fixtures; 365scores fills in otherwise
        playerDetailsByExternalId.setRecentMatches(recentMatchesOf(playerDetailsByExternalId)
                .orElse(playerDetailsResponseByExternalId.getRecentMatches()));
        // fixture-maintained players keep our stats; only externally maintained or flagged ones are
        // refreshed and written (see Player.refreshesExternally)
        if(Player.refreshesExternally(playerDetailsByExternalId.getUpdatedBy(), playerDetailsByExternalId.getMissedMatches())){
            logger.info("player refreshed from external service, external id {} internal id {}", playerId, playerDetailsByExternalId.getId());
            // a flagged player has probably moved: show the club the external service says he plays for
            if (playerDetailsByExternalId.isFlagged()) {
                String clubPhotoUrl = clubPhotoUrl(playerDetailsResponseByExternalId.getClubId());
                playerService.updatePlayerClubFromExternal(playerDetailsByExternalId.getId(),
                        playerDetailsResponseByExternalId.getClubName(), clubPhotoUrl);
                if (playerDetailsResponseByExternalId.getClubName() != null)
                    playerDetailsByExternalId.setExternalServicePlayerClub(playerDetailsResponseByExternalId.getClubName());
                if (clubPhotoUrl != null) playerDetailsByExternalId.setClubPhotoUrl(clubPhotoUrl);
            }
            if(playerDetailsResponseByExternalId.getLeagueStats() != null) {
                playerDetailsByExternalId.setLeagueStats(createLeagueStatsDto(playerDetailsResponseByExternalId.getLeagueStats()));
                playerService.updatePlayerLeagueStatsDetails(playerDetailsByExternalId.getId(),
                        createLeagueStatsDto(playerDetailsResponseByExternalId.getLeagueStats()));
                // league rating just changed: reprice. world cup participants keep their wc price
                // (the dto's frozen wc inputs are untouched); others reflect the new rating
                playerService.priceDto(playerDetailsByExternalId);
            }
        }
        return playerDetailsByExternalId;
    }

    // 365scores club badge url; null when the external service gave no club
    public String clubPhotoUrl(Integer clubId) {
        if (clubId == null) return null;
        return scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresClubsImageEndPoint() + clubId;
    }

    private Optional<List<PlayerRecentMatch>> recentMatchesOf(PlayerDetailsResDto player) {
        return recentMatchService.fromFixtures(player.getId(), player.getUpdatedBy(), player.getMissedMatches());
    }

    public List<PlayerRecentMatch> fetchPlayerLastRatings(Integer playerId) {
        // ours first: a fixture-maintained player with 5 matches on record needs no 365 call at all
        PlayerDetailsResDto stored = playerService.getPlayerDetailsByExternalId(playerId);
        if (stored != null) {
            Optional<List<PlayerRecentMatch>> fromFixtures = recentMatchesOf(stored);
            if (fromFixtures.isPresent()) {
                logger.info("player last ratings from fixtures, external id {} internal id {}", playerId, stored.getId());
                return fromFixtures.get();
            }
        }
        logger.info("fetching player last ratings only from 365 player external id {}", playerId);
        PlayerDetailsDto playerStatsResponse = fetchPlayerEntityFromExternalService(playerId);
        if(playerStatsResponse == null) return new ArrayList<>();

        List<PlayerRecentMatch> recentMatches = playerStatsResponse.getRecentMatches();
        for (PlayerRecentMatch recentMatch : recentMatches) {
            recentMatch.setOpponentTeamPhotoUrl(scoresConfig.getScoresImageBaseUrl() +
                    scoresConfig.getScoresClubsImageEndPoint() + recentMatch.getOpponentTeamId());
        }
        return playerStatsResponse.getRecentMatches();
    }

    public PlayerDetailsDto fetchPlayerEntityFromExternalService(Integer playerId){
        try {
            String url = scoresConfig.getScoresApiBaseUrl() + "/athletes/?appTypeId=5&athletes=" + playerId + "&fullDetails=true";
            logger.info("getting player league stats from 365 scores url {}", url);

            PlayerDetailsDto playerStatsResponse = restTemplate.getForObject(
                    url,
                    PlayerDetailsDto.class);
            if(playerStatsResponse == null) return null;

            List<PlayerRecentMatch> recentMatches = playerStatsResponse.getRecentMatches();
            if(recentMatches == null) recentMatches = new ArrayList<>();
            for (PlayerRecentMatch recentMatch : recentMatches) {
                recentMatch.setOpponentTeamPhotoUrl(scoresConfig.getScoresImageBaseUrl() +
                        scoresConfig.getScoresClubsImageEndPoint() + recentMatch.getOpponentTeamId());
            }

            return playerStatsResponse;

        }catch (Exception e){
            // player details not found for the current season in scores, return default data
            return null;
        }
    }

    public com.yasser.footballersmarket.playerstats.PlayerLeagueStats createLeagueStatsDto(PlayerLeagueStats externalPlayerLeagueStats){
        return new com.yasser.footballersmarket.playerstats.PlayerLeagueStats(externalPlayerLeagueStats.getGoals(),
                externalPlayerLeagueStats.getAssists(), externalPlayerLeagueStats.getAppearances(), externalPlayerLeagueStats.getRating());
    }

    public SearchPlayerEntity searchClosestPlayerByName(String playerName){
        playerName = Unidecode.decode(playerName);
        try {
            List<SearchPlayerEntity> matchedPlayers = searchByPlayerName(playerName);

            if (matchedPlayers == null || matchedPlayers.size() < 1) return null;

            return matchedPlayers.get(0);

        }catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e){
            // player details not found for the current season in scores, return default data
            return null;
        }
    }

    public List<SearchPlayerEntity> searchByPlayerName(String playerName){
        Integer FOOTBALL_SPORT_ID = 1;
        int PLAYER_SEARCH_LIMIT = 5;
        try {
            String simplifiedPlayerName = simplifyPlayerNameForSearchRequest(playerName);
            String url = scoresConfig.getScoresApiBaseUrl() + "/search/?appTypeId=5&query=" + simplifiedPlayerName + "&filter=all";
            logger.info("searching player in 365 scores search url {}", url);

            PleaseSearchResDto playerSearchResponse = restTemplate.getForObject(
                    url,
                    PleaseSearchResDto.class);

            if (playerSearchResponse == null || playerSearchResponse.getAthletes().size() < 1) {
                logger.debug("No players found for name: {}", simplifiedPlayerName);
                return Collections.emptyList();
            }

            List<PlayerSearchEntityDto> playersFound = playerSearchResponse.getAthletes();
            return playersFound.stream()
                    .filter(athlete -> Objects.equals(athlete.getSportId(), FOOTBALL_SPORT_ID))
                    .limit(PLAYER_SEARCH_LIMIT)
                    .map(this::convertToSearchPlayerEntity)
                    .collect(Collectors.toList());

        }catch (RestClientException e){
            logger.error("Error while searching for players: {}", playerName, e);
            return null;
        }
    }

    public FeaturedPlayersResponse getFeaturedPlayers(){
        int PREMIER_LEAGUE_COMPETITION_NUM = 7;
        int SPANISH_LEAGUE_COMPETITION_NUM = 11;
        // alternates league per day, derived from the date rather than kept in a field: every retry
        // within a day asks for the same url, so an attempt that times out warms the very resource
        // the next one needs. also survives restarts and can't race between request threads.
        int selectedCompetitionNum = LocalDate.now().toEpochDay() % 2 == 0 ?
                PREMIER_LEAGUE_COMPETITION_NUM : SPANISH_LEAGUE_COMPETITION_NUM;
        try {

            String url = scoresConfig.getScoresApiBaseUrl() + "/stats/?appTypeId=5&competitions="
                    + selectedCompetitionNum + "&competitors=&withSeasons=false";
            logger.info("getting top players url {}", url);

            return shortTimeoutRestTemplate.getForObject(
                    url,
                    FeaturedPlayersResponse.class);

        }catch (RestClientException ex) {
            logger.error("could not fetch featured players in the given timeout window");
            return null;
        }
    }

    public void addBaseUrlToTrendingPlayersResponse(FeaturedPlayersResponse response){
        response.getFeaturedPlayers().forEach(player -> {
            player.setPlayerImgUrl(scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresPlayersImageEndPoint() + player.getPlayerId());
            player.setPlayerTeamImgUrl(scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresClubsImageEndPoint() + player.getPlayerTeamId());
        });
    }

    public FeaturedMatch getFootballFeaturedMatch(){
        try {

            String url = scoresConfig.getScoresApiBaseUrl() + "/games/featured/?langId=1&sports=1";
            logger.info("getting featured match url {}", url);

            FeaturedMatch featuredMatchRes = restTemplate.getForObject(
                    url,
                    FeaturedMatch.class);

            if (featuredMatchRes == null) return null;

            featuredMatchRes.setHomeTeamImgUrl(scoresConfig.getScoresImageBaseUrl() +
                    scoresConfig.getScoresClubsImageEndPoint() + featuredMatchRes.getHomeTeamId());
            featuredMatchRes.setAwayTeamImgUrl(scoresConfig.getScoresImageBaseUrl() +
                    scoresConfig.getScoresClubsImageEndPoint() + featuredMatchRes.getAwayTeamId());
            return featuredMatchRes;

        }catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e){
            return null;
        }
    }

    private SearchPlayerEntity convertToSearchPlayerEntity(PlayerSearchEntityDto playerDto) {
        return new SearchPlayerEntity(
                playerDto.getId(),
                playerDto.getName(),
                playerDto.getClubName(),
                playerDto.getClubId(),
                scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresPlayersImageEndPoint() + playerDto.getId(),  // empty string as default value
                scoresConfig.getScoresImageBaseUrl() +
                        scoresConfig.getScoresClubsImageEndPoint() + playerDto.getClubId()
        );
    }

    public PlayerDetailsResDto convertDtoToPlayerDetailsDto(Integer playerId,
                                                          PlayerDetailsDto playerStatDetailsDto,
                                                          List<PlayerRecentMatch> recentMatches){
        PlayerDetailsResDto playerDto = new PlayerDetailsResDto();
        playerDto.setExternalServicePlayerId(playerId);
        playerDto.setName(playerStatDetailsDto.getName());
        playerDto.setNationality(playerStatDetailsDto.getNationality());
        playerDto.setDateOfBirth(playerStatDetailsDto.getDateOfBirth());
        playerDto.setPosition(playerStatDetailsDto.getPosition());
        playerDto.setRapidApiClub("");
        playerDto.setExternalServicePlayerClub(playerStatDetailsDto.getClubName());
        playerDto.setPhotoUrl(scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresPlayersImageEndPoint() + playerId);
        playerDto.setClubPhotoUrl(scoresConfig.getScoresImageBaseUrl() + scoresConfig.getScoresClubsImageEndPoint() + playerStatDetailsDto.getClubId());

        com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats leagueStats = playerStatDetailsDto.getLeagueStats();
        if(leagueStats == null){
            // external service has no stats for this player: default rating so he still prices sanely
            playerDto.setLeagueStats(new com.yasser.footballersmarket.playerstats.PlayerLeagueStats(0, 0, 0, 6.5));
        }else{
            com.yasser.footballersmarket.playerstats.PlayerLeagueStats playerEntityLeagueStats =
                    new com.yasser.footballersmarket.playerstats.PlayerLeagueStats(leagueStats.getGoals(),
                    leagueStats.getAssists(), leagueStats.getAppearances(), leagueStats.getRating());
            playerDto.setLeagueStats(playerEntityLeagueStats);
        }


        playerDto.setRecentMatches(recentMatches);

        // not-in-db player: no world cup stats, priced from his league rating
        playerService.priceDto(playerDto);

        return playerDto;
    }

    private String simplifyPlayerNameForSearchRequest(String playerName){
        playerName = Unidecode.decode(playerName);

        if(playerName.contains(".")){
            String[] splitName = playerName.split("\\.");
            String s = splitName[splitName.length - 1];
            return s.replaceAll("\\s", "");
        }
        return playerName;
    }

}
