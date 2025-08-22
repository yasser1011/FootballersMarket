package com.yasser.footballersmarket.player;

import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.scores365.ScoresService;
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

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Autowired
    private PlayerService playerService;

    @MockBean
    private ScoresService scoresService;

    @BeforeEach
    void setUp() {
        playerService.deleteAll();
    }


    @Test
    void shouldReturnPlayerFromDbByPlayerId() {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(0,0,1.0));

        playerService.savePlayer(player1);

        PlayerDetailsResDto player1Res = playerService.getPlayerDetailsById(player1.getId(), 0);

        assertThat(player1Res).isNotNull();
        assertThat(player1Res.getId()).isEqualTo(player1.getId());
        assertThat(player1Res.getExternalServicePlayerId()).isEqualTo(player1.getSofascoreId());
        assertThat(player1Res.getName()).isEqualTo(player1.getName());
    }

    @Test
    void shouldReturnPlayerFromDbByExternalServicePlayerId() {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "club1", null);
        player1.setLeagueStats(new PlayerLeagueStats(0,0,1.0));

        playerService.savePlayer(player1);

        PlayerDetailsResDto player1Res = playerService.getPlayerDetailsById(0L, 10);

        assertThat(player1Res).isNotNull();
        assertThat(player1Res.getId()).isEqualTo(player1.getId());
        assertThat(player1Res.getExternalServicePlayerId()).isEqualTo(player1.getSofascoreId());
        assertThat(player1Res.getName()).isEqualTo(player1.getName());
    }

    @Test
    void shouldReturnPlayerFromExternalServiceByDbExternalServicePlayerId() {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "DifferentClub2", "sofascore");
        player1.setLeagueStats(new PlayerLeagueStats(0,0,1.0));

        playerService.savePlayer(player1);

        Player player1Mocked = new Player(1L, 10, "player1", dob, "club1", "club1", "sofascore");
        player1Mocked.setLeagueStats(new PlayerLeagueStats(1,1,7.0));
        PlayerDetailsResDto mockedPlayerDetails = playerService.convertPlayerEntityToPlayerDto(player1Mocked);
        Mockito
                .when(scoresService.getPlayerEntityFromExternalService(any()))
                .thenReturn(mockedPlayerDetails);

        PlayerDetailsResDto player1Res = playerService.getPlayerDetailsById(0L, player1.getSofascoreId());

        assertThat(player1Res).isNotNull();
        assertThat(player1Res.getId()).isEqualTo(player1.getId());
        assertThat(player1Res.getExternalServicePlayerId()).isEqualTo(player1.getSofascoreId());
        assertThat(player1Res.getName()).isEqualTo(player1.getName());

        assertThat(player1Res.getLeagueStats().getGoals()).isEqualTo(player1Mocked.getLeagueStats().getGoals());
        assertThat(player1Res.getLeagueStats().getAssists()).isEqualTo(player1Mocked.getLeagueStats().getAssists());
        assertThat(player1Res.getLeagueStats().getRating()).isEqualTo(player1Mocked.getLeagueStats().getRating());

        PlayerDetailsResDto updatedPlayerDb = playerService.getPlayerByIdAndSofascoreId(player1.getId(), player1.getSofascoreId());
        assertThat(updatedPlayerDb.getLeagueStats().getGoals()).isEqualTo(player1Mocked.getLeagueStats().getGoals());
        assertThat(updatedPlayerDb.getLeagueStats().getAssists()).isEqualTo(player1Mocked.getLeagueStats().getAssists());
        assertThat(updatedPlayerDb.getLeagueStats().getRating()).isEqualTo(player1Mocked.getLeagueStats().getRating());
    }

    @Test
    void shouldReturnPlayerFromExternalServiceByDbPlayerId() {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "DifferentClub2", "sofascore");
        player1.setLeagueStats(new PlayerLeagueStats(0,0,1.0));

        playerService.savePlayer(player1);

        Player player1Mocked = new Player(1L, 10, "player1", dob, "club1", "club1", "sofascore");
        player1Mocked.setLeagueStats(new PlayerLeagueStats(1,1,7.0));
        PlayerDetailsResDto mockedPlayerDetails = playerService.convertPlayerEntityToPlayerDto(player1Mocked);
        Mockito
                .when(scoresService.getPlayerEntityFromExternalService(any()))
                .thenReturn(mockedPlayerDetails);

        PlayerDetailsResDto player1Res = playerService.getPlayerDetailsById(player1.getId(), 0);

        assertThat(player1Res).isNotNull();
        assertThat(player1Res.getId()).isEqualTo(player1.getId());
        assertThat(player1Res.getExternalServicePlayerId()).isEqualTo(player1.getSofascoreId());
        assertThat(player1Res.getName()).isEqualTo(player1.getName());

        assertThat(player1Res.getLeagueStats().getGoals()).isEqualTo(player1Mocked.getLeagueStats().getGoals());
        assertThat(player1Res.getLeagueStats().getAssists()).isEqualTo(player1Mocked.getLeagueStats().getAssists());
        assertThat(player1Res.getLeagueStats().getRating()).isEqualTo(player1Mocked.getLeagueStats().getRating());

        PlayerDetailsResDto updatedPlayerDb = playerService.getPlayerByIdAndSofascoreId(player1.getId(), player1.getSofascoreId());
        assertThat(updatedPlayerDb.getLeagueStats().getGoals()).isEqualTo(player1Mocked.getLeagueStats().getGoals());
        assertThat(updatedPlayerDb.getLeagueStats().getAssists()).isEqualTo(player1Mocked.getLeagueStats().getAssists());
        assertThat(updatedPlayerDb.getLeagueStats().getRating()).isEqualTo(player1Mocked.getLeagueStats().getRating());
    }

    @Test
    void shouldReturnPlayerFromExternalService() {
        LocalDate dob = LocalDate.of(1998, 1, 8);
        Player player1 = new Player(1L, 10, "player1", dob, "club1", "DifferentClub2", "sofascore");
        player1.setLeagueStats(new PlayerLeagueStats(0,0,1.0));

        playerService.savePlayer(player1);

        Player player1Mocked = new Player(1L, 10, "player1", dob, "club1", "club1", "sofascore");
        player1Mocked.setLeagueStats(new PlayerLeagueStats(1,1,7.0));
        PlayerDetailsResDto mockedPlayerDetails = playerService.convertPlayerEntityToPlayerDto(player1Mocked);
        Mockito
                .when(scoresService.getPlayerEntityFromExternalService(any()))
                .thenReturn(mockedPlayerDetails);

        PlayerDetailsResDto player1Res = playerService.getPlayerDetailsById(player1.getId(), player1.getSofascoreId());

        assertThat(player1Res).isNotNull();
        assertThat(player1Res.getId()).isEqualTo(player1.getId());
        assertThat(player1Res.getExternalServicePlayerId()).isEqualTo(player1.getSofascoreId());
        assertThat(player1Res.getName()).isEqualTo(player1.getName());

        assertThat(player1Res.getLeagueStats().getGoals()).isEqualTo(player1Mocked.getLeagueStats().getGoals());
        assertThat(player1Res.getLeagueStats().getAssists()).isEqualTo(player1Mocked.getLeagueStats().getAssists());
        assertThat(player1Res.getLeagueStats().getRating()).isEqualTo(player1Mocked.getLeagueStats().getRating());

        PlayerDetailsResDto updatedPlayerDb = playerService.getPlayerByIdAndSofascoreId(player1.getId(), player1.getSofascoreId());
        assertThat(updatedPlayerDb.getLeagueStats().getGoals()).isEqualTo(player1Mocked.getLeagueStats().getGoals());
        assertThat(updatedPlayerDb.getLeagueStats().getAssists()).isEqualTo(player1Mocked.getLeagueStats().getAssists());
        assertThat(updatedPlayerDb.getLeagueStats().getRating()).isEqualTo(player1Mocked.getLeagueStats().getRating());
    }
}