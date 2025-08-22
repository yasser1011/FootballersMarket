package com.yasser.footballersmarket.transaction;

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
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.transaction.dto.TransactionRequest;
import com.yasser.footballersmarket.transaction.dto.TransactionResponse;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private UserService userService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SofascoreService sofascoreService;
    @Autowired
    private ScoresService scoresService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private PlayerUserCurrentBoughtService playerUserCurrentBoughtService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());;

    @BeforeEach
    void setUp() {
        playerUserCurrentBoughtService.deleteAll();
        transactionService.deleteAll();
        userService.deleteAll();
        playerService.deleteAll();
    }

    @Test
    void buyTransactionWhenPlayerIsFoundInDBShouldReturnSuccess() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 818244, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(10,2,7.8));
        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(player1);
        User user1 = new User("user1", "pass1");
        user1.setPoints(10000);

        userService.saveUsersList(List.of(user1));
        playerService.savePlayer(player1);

        TransactionRequest transactionRequest = new TransactionRequest(player1.getId(), player1.getSofascoreId(),
                1, playerDetailsResDto.getPrice());

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String transactionResponseStr = mvcResult.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(transactionResponseStr, new TypeReference<>(){});

        assertThat(transactionResponse.getStatus()).isEqualTo(0);

        // verify transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(1);
        Transaction transactionRes = transactions.getContent().get(0);
        assertThat(transactionRes.getTransactionType()).isEqualTo(1);
        assertThat(transactionRes.getPrice()).isEqualTo(playerDetailsResDto.getPrice());
        assertThat(transactionRes.getPlayerId()).isEqualTo(player1.getId());
        assertThat(transactionRes.getUserId()).isEqualTo(user1.getId());
        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(1);
        PlayerUserCurrentBought userPlayer = userPlayers.get(0);
        assertThat(userPlayer.getPlayerId()).isEqualTo(player1.getId());
        assertThat(userPlayer.getUserId()).isEqualTo(user1.getId());
        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints() - playerDetailsResDto.getPrice());
    }

    @Test
    void buyTransactionWhenPlayerIsNotFoundInDBShouldReturnSuccess() throws Exception {
        User user1 = new User("user1", "pass1");
        user1.setPoints(10000);

        userService.saveUsersList(List.of(user1));

//        Player playerFromSofascore = sofascoreService.getPlayerEntityFromSofascore(818244);
//        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(playerFromSofascore);
        PlayerDetailsResDto playerDetailsResDto = scoresService.getPlayerEntityFromExternalService(39779);
        TransactionRequest transactionRequest = new TransactionRequest(null, playerDetailsResDto.getExternalServicePlayerId(),
                1, playerDetailsResDto.getPrice());

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String transactionResponseStr = mvcResult.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(transactionResponseStr, new TypeReference<>(){});

        assertThat(transactionResponse.getStatus()).isEqualTo(0);

        // verify transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(1);
        Transaction transactionRes = transactions.getContent().get(0);
        assertThat(transactionRes.getTransactionType()).isEqualTo(1);
        assertThat(transactionRes.getPrice()).isEqualTo(playerDetailsResDto.getPrice());
        assertThat(transactionRes.getPlayer().getSofascoreId()).isEqualTo(playerDetailsResDto.getExternalServicePlayerId());
        assertThat(transactionRes.getUserId()).isEqualTo(user1.getId());

        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(1);
        PlayerUserCurrentBought userPlayer = userPlayers.get(0);
        assertThat(userPlayer.getPlayerId()).isEqualTo(transactionRes.getPlayerId());
        assertThat(userPlayer.getUserId()).isEqualTo(user1.getId());

        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints() - playerDetailsResDto.getPrice());

        // verify new player is saved
        Page<PlayerDetailsResDto> playersPageInDb = playerService.getHomePagePlayerData(0);
        assertThat(playersPageInDb.getTotalElements()).isEqualTo(1);
        PlayerDetailsResDto playerAdded = playersPageInDb.getContent().get(0);
        assertThat(playerAdded.getExternalServicePlayerId()).isEqualTo(playerDetailsResDto.getExternalServicePlayerId());
        assertThat(playerAdded.getUpdatedBy()).isEqualTo("sofascore_buy");
    }

    @Test
    void buyTransactionWhenUserDoesNotHaveEnoughPointsReturnFailure() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 818244, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(10,2,7.8));
        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(player1);
        User user1 = new User("user1", "pass1");
        user1.setPoints(100);

        userService.saveUsersList(List.of(user1));
        playerService.savePlayer(player1);

        TransactionRequest transactionRequest = new TransactionRequest(player1.getId(), player1.getSofascoreId(),
                1, playerDetailsResDto.getPrice());

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String transactionResponseStr = mvcResult.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(transactionResponseStr, new TypeReference<>(){});

        assertThat(transactionResponse.getStatus()).isEqualTo(-1);
        assertThat(transactionResponse.getError().getErrorMsg()).isEqualTo("not enough points to buy the player");

        // transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(0);

        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(0);

        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints());
    }

    @Test
    void buyTransactionWhenUserAlreadyBoughtSamePlayerReturnFailure() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 818244, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(10,2,7.8));
        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(player1);
        User user1 = new User("user1", "pass1");
        user1.setPoints(10000);

        userService.saveUsersList(List.of(user1));
        playerService.savePlayer(player1);

        // buy transaction
        PlayerUserCurrentBought currBoughtTransaction1 = new PlayerUserCurrentBought(user1.getId(), player1.getId(), 500);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction1);

        TransactionRequest transactionRequest = new TransactionRequest(player1.getId(), player1.getSofascoreId(),
                1, playerDetailsResDto.getPrice());

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String transactionResponseStr = mvcResult.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(transactionResponseStr, new TypeReference<>(){});

        assertThat(transactionResponse.getStatus()).isEqualTo(-1);
        assertThat(transactionResponse.getError().getErrorMsg()).isEqualTo("player is already bought by this user");

        // transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(0);

        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(1);

        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints());
    }

    @Test
    void buyTransactionWhenUserProvideWrongPriceReturnFailure() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 818244, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(10,2,7.8));
        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(player1);
        User user1 = new User("user1", "pass1");
        user1.setPoints(1000);

        userService.saveUsersList(List.of(user1));
        playerService.savePlayer(player1);

        TransactionRequest transactionRequest = new TransactionRequest(player1.getId(), player1.getSofascoreId(),
                1, 200);

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String transactionResponseStr = mvcResult.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(transactionResponseStr, new TypeReference<>(){});

        assertThat(transactionResponse.getStatus()).isEqualTo(-1);
        assertThat(transactionResponse.getError().getErrorMsg()).isEqualTo("wrong price");
        assertThat(transactionResponse.getError().getErrorType()).isEqualTo("price");
        assertThat(transactionResponse.getError().getPlayerPrice()).isEqualTo(playerDetailsResDto.getPrice());

        // transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(0);

        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(0);

        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints());
    }

    @Test
    void sellTransactionShouldReturnSuccess() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 818244, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(10,2,7.8));
        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(player1);
        User user1 = new User("user1", "pass1");
        user1.setPoints(10000);

        userService.saveUsersList(List.of(user1));
        playerService.savePlayer(player1);

        // buy transaction
        transactionService.saveTransactions(List.of(new Transaction(1, 500, LocalDateTime.now(),user1.getId(), player1.getId())));
        PlayerUserCurrentBought currBoughtTransaction1 = new PlayerUserCurrentBought(user1.getId(), player1.getId(), 500);
        playerUserCurrentBoughtService.savePlayerUserCurrBoughtTransaction(currBoughtTransaction1);

        TransactionRequest transactionRequest = new TransactionRequest(player1.getId(), player1.getSofascoreId(),
                2, playerDetailsResDto.getPrice());

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // verify transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(2);
        Transaction transactionRes = transactions.stream().filter(transaction -> transaction.getTransactionType() == 2).findAny().orElseThrow();
        Transaction transactionBuyRes = transactions.stream().filter(transaction -> transaction.getTransactionType() == 1).findAny().orElseThrow();
        assertThat(transactionRes).isNotNull();
        assertThat(transactionBuyRes).isNotNull();
        assertThat(transactionRes.getPrice()).isEqualTo(playerDetailsResDto.getPrice());
        assertThat(transactionRes.getPlayerId()).isEqualTo(player1.getId());
        assertThat(transactionRes.getUserId()).isEqualTo(user1.getId());
        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(0);

        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints() + playerDetailsResDto.getPrice());
    }

    @Test
    void sellTransactionWhenPlayerWasNotBoughtShouldReturnFailure() throws Exception {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 818244, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(10,2,7.8));
        PlayerDetailsResDto playerDetailsResDto = playerService.convertPlayerEntityToPlayerDto(player1);
        User user1 = new User("user1", "pass1");
        user1.setPoints(10000);

        userService.saveUsersList(List.of(user1));
        playerService.savePlayer(player1);

        TransactionRequest transactionRequest = new TransactionRequest(player1.getId(), player1.getSofascoreId(),
                2, playerDetailsResDto.getPrice());

        MvcResult mvcResult = mockMvc.perform(post("/api/transactions")
                        .with(user(user1))
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String transactionResponseStr = mvcResult.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(transactionResponseStr, new TypeReference<>(){});

        assertThat(transactionResponse.getStatus()).isEqualTo(-1);
        assertThat(transactionResponse.getError().getErrorMsg()).isEqualTo("player wasn't bought by this user");

        // transaction table
        Page<Transaction> transactions = transactionService.getTransactions(0);
        assertThat(transactions.getTotalElements()).isEqualTo(0);

        // verify current bought
        List<PlayerUserCurrentBought> userPlayers = playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(user1.getId());
        assertThat(userPlayers.size()).isEqualTo(0);

        // verify user points
        User userReturned = userService.findUserById(user1.getId());
        assertThat(userReturned.getPoints()).isEqualTo(user1.getPoints());
    }
}