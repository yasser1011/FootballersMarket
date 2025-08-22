package com.yasser.footballersmarket.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.anyOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private PlayerService playerService;
    @Autowired
    private UserService userService;
    @Autowired
    private PlayerUserCurrentBoughtService playerUserCurrentBoughtService;
    @Autowired
    private TransactionService transactionService;
    @MockBean
    private ScoresService scoresService;

    @BeforeEach
    void setUp() {
        playerUserCurrentBoughtService.deleteAll();
        userService.deleteAll();
        playerService.deleteAll();
    }

    @Test
    void getTopRankedUsers() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(0,0,1.0));
        Player player2 = new Player(2L, 11, "player2", dob, "club2", "club2", null);
        player2.setLeagueStats(new PlayerLeagueStats(0,0,2.0));
        Player player3 = new Player(3L, 12, "player3", dob, "club2", "club2", null);
        player3.setLeagueStats(new PlayerLeagueStats(0,0,3.0));
        Player player4 = new Player(4L, 13, "player4", dob, "club2", "club2", null);
        player4.setLeagueStats(new PlayerLeagueStats(0,0,4.0));
        Player player5 = new Player(5L, 14, "player5", dob, "club2", "club2", null);
        player5.setLeagueStats(new PlayerLeagueStats(0,0,5.0));
        Player player6 = new Player(6L, 15, "player6", dob, "club2", "club2", null);
        player6.setLeagueStats(new PlayerLeagueStats(0,0,5.0));

        User user1 = new User("user1", "pass1", 100000);
        User user2 = new User("user2", "pass2", 100000);
        User user3 = new User("user3", "pass2", 100000);
        User user4 = new User("user4", "pass2", 100000);
        User user5 = new User("user5", "pass2", 100000);
        User user6 = new User("user6", "pass2", 100000);

        userService.saveUsersList(List.of(user1, user2, user3, user4, user5, user6));
        playerService.savePlayersList(List.of(player1, player2, player3, player4, player5, player6));
        // save player buy transaction
        Integer price1 = playerService.convertPlayerEntityToPlayerDto(player1).getPrice();
        Integer price2 = playerService.convertPlayerEntityToPlayerDto(player2).getPrice();
        Integer price3 = playerService.convertPlayerEntityToPlayerDto(player3).getPrice();
        Integer price4 = playerService.convertPlayerEntityToPlayerDto(player4).getPrice();
        Integer price5 = playerService.convertPlayerEntityToPlayerDto(player5).getPrice();
        Integer price6 = playerService.convertPlayerEntityToPlayerDto(player6).getPrice();
        PlayerUserCurrentBought currBoughtTransaction1 = new PlayerUserCurrentBought(user1.getId(), player1.getId(), price1);
        PlayerUserCurrentBought currBoughtTransaction2 = new PlayerUserCurrentBought(user2.getId(), player2.getId(), price2);
        PlayerUserCurrentBought currBoughtTransaction3 = new PlayerUserCurrentBought(user3.getId(), player3.getId(), price3);
        PlayerUserCurrentBought currBoughtTransaction4 = new PlayerUserCurrentBought(user4.getId(), player4.getId(), price4);
        PlayerUserCurrentBought currBoughtTransaction5 = new PlayerUserCurrentBought(user5.getId(), player5.getId(), price5);
        PlayerUserCurrentBought currBoughtTransaction6 = new PlayerUserCurrentBought(user6.getId(), player5.getId(), price5);
        PlayerUserCurrentBought currBoughtTransaction7 = new PlayerUserCurrentBought(user6.getId(), player6.getId(), price6);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransactions(List.of(currBoughtTransaction1,
                currBoughtTransaction2, currBoughtTransaction3, currBoughtTransaction4, currBoughtTransaction5,
                currBoughtTransaction6, currBoughtTransaction7));

        MvcResult mvcResult = mockMvc.perform(get("/api/users/top-users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        List<UserResponse> jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        assertThat(jsonResponse.size()).isEqualTo(5);
        assertThat(jsonResponse.get(0).getUserId()).isEqualTo(user6.getId());
        assertThat(jsonResponse.get(0).getPoints()).isEqualTo(
                user6.getPoints() + price5 +
                        price6);
    }

    @Test
    void getAllUsersScores() throws Exception {
        User user1 = new User("user1", "pass1", 100);
        User user2 = new User("user2", "pass2", 100);

        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "club1", "sofascore");
        player1.setLeagueStats(new PlayerLeagueStats(0,0,5.0));
        Player player2 = new Player(2L, 11, "player2", dob, "club2", "club2", null);
        player2.setLeagueStats(new PlayerLeagueStats(0,0,2.0));
        Player player3 = new Player(3L, 12, "player3", dob, "club2", "club2", null);
        player3.setLeagueStats(new PlayerLeagueStats(0,0,3.0));

        Player player1Mocked = new Player(1L, 10, "player1", dob, "club1", "club1", "sofascore");
        player1Mocked.setLeagueStats(new PlayerLeagueStats(1,0,7.0));
        PlayerDetailsResDto mockedPlayerDetails = playerService.convertPlayerEntityToPlayerDto(player1Mocked);

        userService.saveUsersList(List.of(user1, user2));
        playerService.savePlayersList(List.of(player1, player2, player3));
        // save player buy transaction
        Integer price1 = playerService.convertPlayerEntityToPlayerDto(player1).getPrice();
        Integer price2 = playerService.convertPlayerEntityToPlayerDto(player2).getPrice();
        Integer price3 = playerService.convertPlayerEntityToPlayerDto(player3).getPrice();

        PlayerUserCurrentBought currBoughtTransaction1 = new PlayerUserCurrentBought(user1.getId(), player1.getId(), price1);
        PlayerUserCurrentBought currBoughtTransaction2 = new PlayerUserCurrentBought(user1.getId(), player2.getId(), price2);
        PlayerUserCurrentBought currBoughtTransaction3 = new PlayerUserCurrentBought(user2.getId(), player3.getId(), price3);

        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransactions(List.of(currBoughtTransaction1,
                currBoughtTransaction2, currBoughtTransaction3));

        Mockito
                .when(scoresService.getPlayerEntityFromExternalService(any()))
                .thenReturn(mockedPlayerDetails);

        MvcResult mvcResult = mockMvc.perform(get("/api/users/rankings"))
                .andExpect(status().isOk())
                .andReturn();

        String stringResponse = mvcResult.getResponse().getContentAsString();
        List<UserResponse> jsonResponse = objectMapper.readValue(stringResponse, new TypeReference<>(){});

        assertThat(jsonResponse.size()).isEqualTo(2);
        Optional<UserResponse> user1Res = jsonResponse.stream().filter(user -> user.getUserId() == user1.getId()).findFirst();
        assertThat(user1Res).isNotEmpty();
        Optional<UserResponse> user2Res = jsonResponse.stream().filter(user -> user.getUserId() == user2.getId()).findFirst();
        assertThat(user2Res).isNotEmpty();

        UserResponse user1ResObj = user1Res.get();
        UserResponse user2ResObj = user2Res.get();
        assertThat(user1ResObj.getPoints()).isEqualTo(
                user1.getPoints() + price2 +
                        mockedPlayerDetails.getPrice());
        assertThat(user2ResObj.getPoints()).isEqualTo(
                user2.getPoints() + price3);

    }
}