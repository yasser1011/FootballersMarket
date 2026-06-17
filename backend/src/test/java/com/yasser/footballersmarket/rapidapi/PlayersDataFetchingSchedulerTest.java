package com.yasser.footballersmarket.rapidapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStatsRepository;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsRepository;
import com.yasser.footballersmarket.rapidapi.dto.*;
import com.yasser.footballersmarket.scores365.dto.PleaseSearchResDto;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class PlayersDataFetchingSchedulerTest extends BaseIntegrationTest {

    @Autowired
    private PlayersDataFetchingScheduler playersDataFetchingScheduler;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private PlayerLeagueStatsRepository playerLeagueStatsRepository;
    @Autowired
    private PlayerChampionsLeagueStatsRepository playerChampionsLeagueStatsRepository;

    private MockRestServiceServer mockServer;

//    @Autowired
    // if you have specific conf for restTemplate, and you need it in test use @Autowired with mockServer
    @MockBean
    private RestTemplate restTemplate;
    @Value("classpath:search_response.json")
    private Resource searchQueryResponseJson;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private RapidApiConfig rapidApiConfig;

    @BeforeEach
    void setUp() {
        // no need to have MockRestServiceServer since my rest template is simple
//        mockServer = MockRestServiceServer.createServer(restTemplate);

        playerChampionsLeagueStatsRepository.deleteAll();
        playerLeagueStatsRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void shouldUpdatePlayerDataAfterSecondSchedulerRun() throws Exception {
        PlayerStats firstRunPlayerStats = new PlayerStats("bayern", 2, "ok", "5", 4, 4);
        PlayerStats secondRunPlayerStats = new PlayerStats("bayern", 3, "ok", "6", 6, 6);

        PlayerInfo basicTestInfo = new PlayerInfo(1L, "kane", LocalDate.now(), "ok", "ok", false, 23, "kane", "ok");
        PlayerResponse playerFirstRunResponse = new PlayerResponse(basicTestInfo, List.of(firstRunPlayerStats));
        PlayerResponse playerSecondRunResponse = new PlayerResponse(basicTestInfo, List.of(secondRunPlayerStats));
        PlayersApiResponse firstRunApiResponse = new PlayersApiResponse(List.of(playerFirstRunResponse), 1);
        PlayersApiResponse secondRunApiResponse = new PlayersApiResponse(List.of(playerSecondRunResponse), 1);

        when(restTemplate.exchange(anyString(), any(), any(), eq(PlayersApiResponse.class)))
                .thenReturn(new ResponseEntity<>(firstRunApiResponse, HttpStatus.OK));

        PleaseSearchResDto searchQueryResDto = objectMapper.readValue(searchQueryResponseJson.getInputStream(), PleaseSearchResDto.class);
        when(restTemplate.getForObject(anyString(), eq(PleaseSearchResDto.class)))
                .thenReturn(searchQueryResDto);

        HashMap<String, String> bayernTeamConfig = new HashMap<>();
        bayernTeamConfig.put("", "");
        when(rapidApiConfig.getSchedulerTeamsSearchInput())
                .thenReturn(List.of(bayernTeamConfig));

        playersDataFetchingScheduler.rapidApiSchedulerRunner();
        // List<PlayerLeagueStats> firstRes = playerLeagueStatsRepository.findAll();

        when(restTemplate.exchange(anyString(), any(), any(), eq(PlayersApiResponse.class)))
                .thenReturn(new ResponseEntity<>(secondRunApiResponse, HttpStatus.OK));
        playersDataFetchingScheduler.rapidApiSchedulerRunner();
        List<PlayerLeagueStats> secondRes = playerLeagueStatsRepository.findAll();

        PlayerLeagueStats playerLeagueStats = secondRes.get(0);
        assertThat(playerLeagueStats).isNotNull();
        assertThat(playerLeagueStats.getGoals()).isEqualTo(secondRunPlayerStats.getGoals());

    }
}