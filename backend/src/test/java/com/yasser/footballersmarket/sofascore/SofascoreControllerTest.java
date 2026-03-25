package com.yasser.footballersmarket.sofascore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.featuredplayer.FeaturedPlayer;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.sofascore.dto.SearchPlayerEntity;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayer;
import com.yasser.footballersmarket.sofascore.dto.TrendingPlayersResponse;
import me.xuender.unidecode.Unidecode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class SofascoreControllerTest {


    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

//    @Autowired
//    @MockBean
    @SpyBean
    private SofascoreService sofascoreService;
    @Mock
    private PlayerService playerService;

    // injecting @Mocks annotations into the controller
//    @InjectMocks
//    private SofascoreController sofascoreController;



//    @Test
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