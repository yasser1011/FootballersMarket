package com.yasser.footballersmarket.playerusercurrentbought;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.security.JwtService;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import com.yasser.footballersmarket.user.AuthService;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserService;
import com.yasser.footballersmarket.user.dto.AuthRequest;
import com.yasser.footballersmarket.user.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//@WebMvcTest(PlayerUserCurrentBoughtController.class)
//@SpringBootTest
//@TestPropertySource(
//        locations = "classpath:application-integrationtest.properties")
@AutoConfigureMockMvc
//@ExtendWith(MockitoExtension.class)
class PlayerUserCurrentBoughtControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private PlayerService playerService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private PlayerUserCurrentBoughtService playerUserCurrentBoughtService;

    @BeforeEach
    void setUp() {
        playerUserCurrentBoughtService.deleteAll();
        userService.deleteAll();
        playerService.deleteAll();
    }

    private static Player aPlayer(){
        Player player = new Player();
        player.setId(20L);
        player.setName("palmer");
        player.setLeagueStats(new PlayerLeagueStats(2, 3, 8.0));
        player.setSofascoreId(982780);
        player.setRapidApiClub("club");
        player.setNationality("nationality");

        return player;
    }

    private static User aUser(){
        return new User("username", "password");
    }

    @Test
//    @WithMockUser
    void shouldReturnPlayerIsCurrentlyBoughtByUser() throws Exception {
        Player player = aPlayer();
        // save player
        playerService.savePlayer(player);
        User user = aUser();

        // save user
//        AuthResponse savedUserResp = authService.registerUser(
//                new AuthRequest(user.getUsername(), user.getPassword())
//        );
//        user.setId(savedUserResp.getUser().getUserId());

        userService.saveUsersList(List.of(user));
        Long playerId = player.getId();

        // save player buy transaction
        PlayerUserCurrentBought currBoughtTransaction = new PlayerUserCurrentBought(user.getId(), playerId, 500);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction);

        MvcResult mvcResult = mockMvc.perform(get("/api/users/player-transaction-type?playerId=" + playerId)
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        PlayerUserCurrentBoughtResponse jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        assertThat(jsonResponse.getPlayerId()).isEqualTo(playerId);
        assertThat(jsonResponse.getPlayerCurrentlyBoughtByUser()).isTrue();
        assertThat(jsonResponse.getUserId()).isEqualTo(user.getId());
        assertThat(jsonResponse.getBuyPrice()).isEqualTo(500);

    }

    @Test
    void shouldReturnPlayerIsNotCurrentlyBoughtByUser() throws Exception {
        Player player = aPlayer();
        // save player
        playerService.savePlayer(player);
        User user = aUser();

        // save user
//        AuthResponse savedUserResp = authService.registerUser(
//                new AuthRequest(user.getUsername(), user.getPassword())
//        );
//        user.setId(savedUserResp.getUser().getUserId());

        userService.saveUsersList(List.of(user));

        Long playerId = player.getId();

        MvcResult mvcResult = mockMvc.perform(get("/api/users/player-transaction-type?playerId=" + playerId)
                        .with(user(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        PlayerUserCurrentBoughtResponse jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        assertThat(jsonResponse.getPlayerId()).isEqualTo(playerId);
        assertThat(jsonResponse.getPlayerCurrentlyBoughtByUser()).isFalse();
        assertThat(jsonResponse.getUserId()).isEqualTo(user.getId());
        assertThat(jsonResponse.getBuyPrice()).isNull();

    }

    @Test
    void shouldReturnUnauthorizedUserPlayerIsCurrentlyBoughtByUser() throws Exception {
        Player player = aPlayer();
        Long playerId = player.getId();

        mockMvc.perform(get("/api/users/player-transaction-type?playerId=" + playerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

    }


    @Test
    void getUserTeam() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(0,0,6.5));
        // dembele sofascore id
        Player player2 = new Player(2L, 39779, "player2", dob, "club2", "club2", "sofascore");
        player2.setLeagueStats(new PlayerLeagueStats(0,0,6.5));
        List<Player> user1Players = Arrays.asList(player1, player2);

        Player player3 = new Player(3L, 30, "player3", dob,  "club3", "club3", null);
        player3.setLeagueStats(new PlayerLeagueStats(0,0,6.5));

        User user1 = new User("user1", "pass1");
        User user2 = new User("user2", "pass2");

        userService.saveUsersList(List.of(user1, user2));
        playerService.savePlayersList(List.of(player1, player2, player3));

        // save player buy transaction
        PlayerUserCurrentBought currBoughtTransaction1 = new PlayerUserCurrentBought(user1.getId(), player1.getId(), 500);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction1);

        PlayerUserCurrentBought currBoughtTransaction2 = new PlayerUserCurrentBought(user1.getId(), player2.getId(), 500);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction2);

        PlayerUserCurrentBought currBoughtTransaction3 = new PlayerUserCurrentBought(user2.getId(), player3.getId(), 500);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction3);


        MvcResult mvcResult = mockMvc.perform(get("/api/users/myteam")
                        .with(user(user1))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        List<PlayerUserCurrentBoughtDto> jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});
        // compare response list to actual list
        HashMap<Long, Integer> user1PlayerResponseMap = new HashMap<>();
        for (PlayerUserCurrentBoughtDto playerRes: jsonResponse){
            // id, sofascore id, name, club, updated by
            Long playerId = playerRes.getId();
            Integer occurrences = user1PlayerResponseMap.get(playerId);
            user1PlayerResponseMap.put(playerId, occurrences == null ? 1: occurrences + 1);
        }
        boolean areTwoListsEqual = true;
        for (Player player : user1Players) {
            Long playerId = player.getId();
            Integer playerOccurrence = user1PlayerResponseMap.get(playerId);
            if (playerOccurrence == null || playerOccurrence > 1){
                areTwoListsEqual = false;
                break;
            }
            user1PlayerResponseMap.remove(playerId);
        }
        assertThat(areTwoListsEqual).isTrue();
        assertThat(user1PlayerResponseMap.isEmpty()).isTrue();

    }

}