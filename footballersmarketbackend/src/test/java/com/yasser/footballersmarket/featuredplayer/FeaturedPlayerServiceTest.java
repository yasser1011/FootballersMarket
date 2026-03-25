package com.yasser.footballersmarket.featuredplayer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayer;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayersResponse;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1- get players from sofascore by mocking and make sure players were not found in db with today's date
// 2- get players from db with today's date players

//@SpringBootTest
//@TestPropertySource(
//        locations = "classpath:application-integrationtest.properties")
@AutoConfigureMockMvc
//@ExtendWith(MockitoExtension.class)
class FeaturedPlayerServiceTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FeaturedPlayerRepository featuredPlayerRepository;
    @MockBean
    private SofascoreService sofascoreService;
    @MockBean
    private ScoresService scoresService;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        featuredPlayerRepository.deleteAll();
    }

    @Test
    void shouldFetchPlayersFromExternalServiceWhenNoPlayerSavedInDbWithSameCurrDate() throws Exception {
        FeaturedPlayer featuredPlayer1 = new FeaturedPlayer("player1", 1,
                "5.5", 1, LocalDate.of(2025, 4, 4), "", "");
        FeaturedPlayer featuredPlayer2 = new FeaturedPlayer("player2", 2,
                "5.5", 2, LocalDate.of(2025, 4, 4), "", "");
        FeaturedPlayer featuredPlayer3 = new FeaturedPlayer("player3", 3,
                "5.5", 3, LocalDate.of(2025, 4, 2), "", "");
        featuredPlayerRepository.saveAll(List.of(featuredPlayer1, featuredPlayer2, featuredPlayer3));


        FeaturedPlayer externalSrvcFeaturedPlayer1 = new FeaturedPlayer("player10", 10,
                "", 10,"5.5", "test");
        FeaturedPlayer externalSrvcFeaturedPlayer2 = new FeaturedPlayer("player11", 11,
                "",10,"5.5", "test");
        FeaturedPlayersResponse trendingPlayersResponseSof = new FeaturedPlayersResponse();
        trendingPlayersResponseSof.setFeaturedPlayers(List.of(externalSrvcFeaturedPlayer1, externalSrvcFeaturedPlayer2));
//        Mockito
//                .when(sofascoreService.getTrendingPlayers())
//                .thenReturn(trendingPlayersResponseSof);
        Mockito
                .when(scoresService.getFeaturedPlayers())
                .thenReturn(trendingPlayersResponseSof);

        MvcResult mvcResult = mockMvc.perform(get("/api/featured-players"))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        FeaturedPlayersResponse jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        List<FeaturedPlayer> featuredPlayersResponseResult = jsonResponse.getFeaturedPlayers();
        assertThat(featuredPlayersResponseResult.size()).isEqualTo(trendingPlayersResponseSof.getFeaturedPlayers().size());
        FeaturedPlayer player1ResponseRes = featuredPlayersResponseResult.get(0);
        FeaturedPlayer player2ResponseRes = featuredPlayersResponseResult.get(1);
        assertThat(player1ResponseRes.getPlayerId()).isEqualTo(externalSrvcFeaturedPlayer1.getPlayerId());
        assertThat(player1ResponseRes.getPlayerName()).isEqualTo(externalSrvcFeaturedPlayer1.getPlayerName());
        assertThat(player2ResponseRes.getPlayerId()).isEqualTo(externalSrvcFeaturedPlayer2.getPlayerId());
        assertThat(player2ResponseRes.getPlayerName()).isEqualTo(externalSrvcFeaturedPlayer2.getPlayerName());
    }

    @Test
    void shouldFetchPlayersFromDbWhenCurrentDateIsAvailable() throws Exception {
        FeaturedPlayer featuredPlayer1 = new FeaturedPlayer("player1", 1,
                "5.5", 1, LocalDate.now(), "", "");
        FeaturedPlayer featuredPlayer2 = new FeaturedPlayer("player2", 2,
                "5.5", 2, LocalDate.now(), "", "");
        FeaturedPlayer featuredPlayer3 = new FeaturedPlayer("player3", 3,
                "5.5", 3, LocalDate.now(), "", "");
        List<FeaturedPlayer> featuredPlayersDb = List.of(featuredPlayer1, featuredPlayer2, featuredPlayer3);
        featuredPlayerRepository.saveAll(featuredPlayersDb);


        FeaturedPlayer externalSrvcFeaturedPlayer1 = new FeaturedPlayer("player10", 10,
                "", 10,"5.5", "test");
        FeaturedPlayer externalSrvcFeaturedPlayer2 = new FeaturedPlayer("player11", 11,
                "",10,"5.5", "test");
        FeaturedPlayersResponse trendingPlayersResponseSof = new FeaturedPlayersResponse();
        trendingPlayersResponseSof.setFeaturedPlayers(List.of(externalSrvcFeaturedPlayer1, externalSrvcFeaturedPlayer2));
//        Mockito
//                .when(sofascoreService.getTrendingPlayers())
//                .thenReturn(trendingPlayersResponseSof);

        Mockito
                .when(scoresService.getFeaturedPlayers())
                .thenReturn(trendingPlayersResponseSof);

        MvcResult mvcResult = mockMvc.perform(get("/api/featured-players"))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        FeaturedPlayersResponse jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        List<FeaturedPlayer> featuredPlayersResponseResult = jsonResponse.getFeaturedPlayers();
        assertThat(featuredPlayersResponseResult.size()).isEqualTo(featuredPlayersDb.size());
        FeaturedPlayer player1ResponseRes = featuredPlayersResponseResult.get(0);
        FeaturedPlayer player2ResponseRes = featuredPlayersResponseResult.get(1);
        FeaturedPlayer player3ResponseRes = featuredPlayersResponseResult.get(2);
        assertThat(player1ResponseRes.getPlayerId()).isEqualTo(featuredPlayer1.getPlayerId());
        assertThat(player1ResponseRes.getPlayerName()).isEqualTo(featuredPlayer1.getPlayerName());
        assertThat(player2ResponseRes.getPlayerId()).isEqualTo(featuredPlayer2.getPlayerId());
        assertThat(player2ResponseRes.getPlayerName()).isEqualTo(featuredPlayer2.getPlayerName());
        assertThat(player3ResponseRes.getPlayerId()).isEqualTo(featuredPlayer3.getPlayerId());
        assertThat(player3ResponseRes.getPlayerName()).isEqualTo(featuredPlayer3.getPlayerName());
    }
}