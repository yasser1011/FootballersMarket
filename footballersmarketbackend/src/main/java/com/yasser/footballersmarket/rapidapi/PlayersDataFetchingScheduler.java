package com.yasser.footballersmarket.rapidapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerBasic;
import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import com.yasser.footballersmarket.playerstats.*;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.rapidapi.dto.PlayerResponse;
import com.yasser.footballersmarket.rapidapi.dto.PlayerStats;
import com.yasser.footballersmarket.rapidapi.dto.PlayersApiResponse;
import com.yasser.footballersmarket.rapidapi.dto.PlayersDataSavingDto;
import com.yasser.footballersmarket.scores365.ScoresConfig;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.transaction.Transaction;
import com.yasser.footballersmarket.transaction.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;

@Service
public class PlayersDataFetchingScheduler {
    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;
    private final ScoresConfig scoresConfig;
    private final Util util;
    private final PlayersDataFetchingProcessingService playersDataFetchingProcessing;
    private final PlayersDataSavingService playersDataSavingService;
    private final SofascoreService sofascoreService;
    private final ScoresService scoresService;
    private final PlayerService playerService;
    private final PlayerLeagueStatsService playerLeagueStatsService;
    private final PlayerChampionsLeagueStatsService playerChampionsLeagueStatsService;
    private final TransactionService transactionService;
    private final PlayerUserCurrentBoughtService playerUserCurrBoughtService;
    @Value("${sofascoreApiBaseUrl}")
    private String sofascoreApiBaseUrl;
    @Value("${summer-transfer-market-end}")
    private String summerTransferMarketEndDateStr;
    @Value("${winter-transfer-market-end}")
    private String winterTransferMarketEndDateStr;

    Logger logger = LoggerFactory.getLogger(PlayersDataFetchingScheduler.class);

    // hashmap to check if player has many stats from different leagues
    // holds the players from all leagues
    private final HashMap<Long, PlayerLeagueStats> playersMap = new HashMap<>();
    private final HashMap<Long, String> multiplePlayerRecordsInSchedulerMap = new HashMap<>();

    public PlayersDataFetchingScheduler(WebClient webClient, RapidApiConfig rapidApiConfig,
                                        PlayersDataFetchingProcessingService playersDataFetchingProcessing,
                                        PlayersDataSavingService playersDataSavingService,
                                        SofascoreService sofascoreService,
                                        PlayerService playerService,
                                        PlayerLeagueStatsService playerLeagueStatsService,
                                        PlayerChampionsLeagueStatsService playerChampionsLeagueStatsService,
                                        TransactionService transactionService,
                                        PlayerUserCurrentBoughtService playerUserCurrBoughtService,
                                        RestTemplate restTemplate,
                                        ScoresService scoresService,
                                        ScoresConfig scoresConfig,
                                        Util util) {
        this.webClient = webClient;
        this.rapidApiConfig = rapidApiConfig;
        this.scoresConfig = scoresConfig;
        this.playersDataFetchingProcessing = playersDataFetchingProcessing;
        this.playersDataSavingService = playersDataSavingService;
        this.sofascoreService = sofascoreService;
        this.playerService = playerService;
        this.playerLeagueStatsService = playerLeagueStatsService;
        this.transactionService = transactionService;
        this.playerChampionsLeagueStatsService = playerChampionsLeagueStatsService;
        this.playerUserCurrBoughtService = playerUserCurrBoughtService;
        this.restTemplate = restTemplate;
        this.scoresService = scoresService;
        this.util = util;
    }

    // 2 am every day
    @Scheduled(cron="0 0 0 * * *", zone="Europe/Istanbul")
//    @Scheduled(initialDelay = 500, fixedDelay = Long.MAX_VALUE)
    public void rapidApiSchedulerRunner() throws InterruptedException, JsonProcessingException {
        util.logMemoryUsage("Start of Scheduler");

        List<HashMap<String, String>> schedulerTeamsSearchInput = rapidApiConfig.getSchedulerTeamsSearchInput();
        for (HashMap<String, String> searchTeam: schedulerTeamsSearchInput){
            int totalNumPages = 1;
            for (int i = 1; i <= totalNumPages; i++){
                String pageNum = String.valueOf(i);
                logger.info("page number {}", pageNum);
                String fullUrl = buildFetchingUrl(pageNum, searchTeam).toString();

                PlayersApiResponse playersDataResponse = fetchPlayersData(fullUrl);
                // for rate limiting

                if(playersDataResponse != null){
                    logger.info("page number {} finished", pageNum);
                    playersDataFetchingProcessing.addFetchedPlayersList(playersDataResponse.getResponse());
                    totalNumPages = playersDataResponse.getTotalNoOfPages();
                }
                Thread.sleep(10000);
            }
        }
        util.logMemoryUsage("After playersMap creation");
//        savePlayersMap();
        // process and clean the fetched players map
        PlayersDataSavingDto playersDataSavingDto = playersDataFetchingProcessing.triggerPlayersMapProcess();
        util.logMemoryUsage("Before savePlayersData");
        // send dto to saving service
        playersDataSavingService.savePlayersData(playersDataSavingDto);
    }


    private PlayersApiResponse fetchPlayersData(String fullUrl) throws InterruptedException, JsonProcessingException {
        // premier league id -> 39
        // league=140&season=2023&team=529
        logger.info("fetching url {}", fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        // scheduler, fetching data, processing (create entities and push to map), persisting (save players map)

        try {
            ResponseEntity<PlayersApiResponse> res = restTemplate.exchange(
                    String.valueOf(fullUrl), HttpMethod.GET, requestEntity, PlayersApiResponse.class);

            if(res.getBody() == null){
                logger.error("error in fetching players data url: {}", fullUrl);
                return null;
            }

            logger.info("URL: {} | Status: {} | list Length: {}",
                    fullUrl,
                    res.getStatusCode(),
                    res.getBody().getResponse().size());
            return res.getBody();
        }catch (Exception e){
            logger.error(e.getMessage());
            return null;
        }

    }

    private StringBuilder buildFetchingUrl(String pageNum, HashMap<String, String> urlInputParams){
        StringBuilder fullUrl = new StringBuilder(rapidApiConfig.getRapidApiUrl() + "/players?");

        for (Map.Entry<String, String> inputParam : urlInputParams.entrySet()) {
            String paramKey = inputParam.getKey();
            String paramValue = inputParam.getValue();
            fullUrl.append(paramKey).append("=").append(paramValue).append("&");
        }
        if(pageNum != null)
            fullUrl.append("page=").append(pageNum);

        return fullUrl;
    }


}
