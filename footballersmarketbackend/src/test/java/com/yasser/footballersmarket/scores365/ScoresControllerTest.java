package com.yasser.footballersmarket.scores365;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayer;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayersResponse;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import me.xuender.unidecode.Unidecode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@SpringBootTest
//@TestPropertySource(
//        locations = "classpath:application-integrationtest.properties")
@AutoConfigureMockMvc
//@ExtendWith(MockitoExtension.class)
class ScoresControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @SpyBean
    private ScoresService scoresService;

    @Test
    // can work with MockBean as it's only mocking the method
    void shouldReturnTrendingPlayers() throws Exception {
        // test response manually need img url not club id and player id alone
        FeaturedPlayersResponse trendingPlayersResponseSof = new FeaturedPlayersResponse();
        TrendingPlayer trendingPlayer = new TrendingPlayer("Pedri",
                992587,"barcelona","test","8",2817);
        FeaturedPlayer featuredPlayer = new FeaturedPlayer("Pedri", 2, "8.0", 2, "", "");
        trendingPlayersResponseSof.setFeaturedPlayers(List.of(featuredPlayer));

        Mockito
                .when(scoresService.getFeaturedPlayers())
                .thenReturn(trendingPlayersResponseSof);

        MvcResult mvcResult = mockMvc.perform(get("/api/ss/trending-players"))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        FeaturedPlayersResponse jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        assertThat(jsonResponse.getFeaturedPlayers().size()).isEqualTo(trendingPlayersResponseSof.getFeaturedPlayers().size());
        assertThat(jsonResponse.getFeaturedPlayers().get(0).getPlayerName())
                .isEqualTo(trendingPlayersResponseSof.getFeaturedPlayers().get(0).getPlayerName());
        assertThat(jsonResponse.getFeaturedPlayers().get(0).getPlayerTeamId())
                .isEqualTo(trendingPlayersResponseSof.getFeaturedPlayers().get(0).getPlayerTeamId());
        assertThat(jsonResponse.getFeaturedPlayers().get(0).getRating())
                .isEqualTo(trendingPlayersResponseSof.getFeaturedPlayers().get(0).getRating());
    }

    @Test
    // can work with @Autowired because we're using actual implementation result
    void shouldSearchByPlayerName() throws Exception {
        String player1NameSubStr = "Lewa";

        MvcResult mvcResult1 = mockMvc.perform(get("/api/ss/players/search?playerName=" + player1NameSubStr))
                .andExpect(status().isOk())
                .andReturn();
        String stringResponse1 = mvcResult1.getResponse().getContentAsString();
        List<SearchPlayerEntity> jsonResponse1 = objectMapper.readValue(stringResponse1, new TypeReference<>(){});
        SearchPlayerEntity player1Match = jsonResponse1.get(0);
        assertThat(Unidecode.decode(player1Match.getName()).toLowerCase()).contains("lewandowski");

        Thread.sleep(5000);
        String player2NameSubStr = "lami";
        MvcResult mvcResult2 = mockMvc.perform(get("/api/ss/players/search?playerName=" + player2NameSubStr))
                .andExpect(status().isOk())
                .andReturn();
        String stringResponse2 = mvcResult2.getResponse().getContentAsString();
        List<SearchPlayerEntity> jsonResponse2 = objectMapper.readValue(stringResponse2, new TypeReference<>(){});
        SearchPlayerEntity player2Match = jsonResponse2.get(0);
        assertThat(Unidecode.decode(player2Match.getName()).toLowerCase()).contains("lamine");
    }
}