package com.yasser.footballersmarket.scores365;


import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerRepository;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.scores365.dto.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStatsRepository;
import com.yasser.footballersmarket.scores365.dto.PlayerDetailsDto;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.parameters.P;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ScoresServiceTest extends BaseIntegrationTest {

    @Autowired
    private ScoresService scoresService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private PlayerLeagueStatsRepository playerLeagueStatsRepository;
    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        playerLeagueStatsRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void shouldReturnPlayerDbDataIfExternalServiceReturnedAnErrorEvenIfStatsAreNotUpdated() {
        when(restTemplate.getForObject(anyString(), eq(PlayerDetailsDto.class)))
                .thenThrow(new RestClientException("exception"));
        Player player = new Player(1L, 20,  "bayern", "tottenham","photo url", "club photo");
        player.setName("kane");
        com.yasser.footballersmarket.playerstats.PlayerLeagueStats playerLeagueStats = new
                com.yasser.footballersmarket.playerstats.PlayerLeagueStats(1, 1, 2, 8.3);
        player.setLeagueStats(playerLeagueStats);
        playerRepository.save(player);

        PlayerDetailsResDto playerDetailsById = scoresService.getPlayerDetailsById(player.getSofascoreId());
        assertThat(playerDetailsById).isNotNull();
        assertThat(playerDetailsById.getLeagueStats()).isNotNull();
        assertThat(playerDetailsById.getLeagueStats().getRating()).isEqualTo(playerLeagueStats.getRating());
        assertThat(playerDetailsById.getLeagueStats().getGoals()).isEqualTo(playerLeagueStats.getGoals());


    }

    @Test
    void shouldReturnDataFromWebServiceIfClubsAreNotMatchingAndUpdateDb(){
        PlayerDetailsDto playerWebServiceResponse = new PlayerDetailsDto();
        PlayerLeagueStats playerLeagueStatsWebServiceResponse = new PlayerLeagueStats(2, 2, 2, 6.7);
        playerWebServiceResponse.setName("Mr.Kane");
        playerWebServiceResponse.setLeagueStats(playerLeagueStatsWebServiceResponse);
        when(restTemplate.getForObject(anyString(), eq(PlayerDetailsDto.class)))
                .thenReturn(playerWebServiceResponse);

        Player player = new Player(1L, 20,  "bayern", "tottenham","photo url", "club photo");
        player.setName("kane");
        com.yasser.footballersmarket.playerstats.PlayerLeagueStats playerLeagueStats = new
                com.yasser.footballersmarket.playerstats.PlayerLeagueStats(1, 1, 2, 8.3);
        player.setLeagueStats(playerLeagueStats);
        playerRepository.save(player);

        PlayerDetailsResDto playerDetailsById = scoresService.getPlayerDetailsById(player.getSofascoreId());
        assertThat(playerDetailsById).isNotNull();
        assertThat(playerDetailsById.getLeagueStats()).isNotNull();
        assertThat(playerDetailsById.getLeagueStats().getRating()).isEqualTo(playerLeagueStatsWebServiceResponse.getRating());
        assertThat(playerDetailsById.getLeagueStats().getGoals()).isEqualTo(playerLeagueStatsWebServiceResponse.getGoals());
        assertThat(playerDetailsById.getName()).isEqualTo(player.getName());

        Player playerById = playerRepository.findPlayerById(1L);
        assertThat(playerById.getUpdatedBy()).isEqualTo("sofascore");
        assertThat(playerById.getLeagueStats()).isNotNull();
        assertThat(playerById.getLeagueStats().getGoals()).isEqualTo(playerLeagueStatsWebServiceResponse.getGoals());
    }

    @Test
    void shouldReturnDataFromDatabaseIfClubsNamesAreMatching(){
        PlayerDetailsDto playerWebServiceResponse = new PlayerDetailsDto();
        PlayerLeagueStats playerLeagueStatsWebServiceResponse = new PlayerLeagueStats(2, 2, 2, 6.7);
        playerWebServiceResponse.setName("Mr.Kane");
        playerWebServiceResponse.setLeagueStats(playerLeagueStatsWebServiceResponse);
        when(restTemplate.getForObject(anyString(), eq(PlayerDetailsDto.class)))
                .thenReturn(playerWebServiceResponse);

        Player player = new Player(1L, 20,  "bayern", "bayern","photo url", "club photo");
        player.setName("kane");
        com.yasser.footballersmarket.playerstats.PlayerLeagueStats playerLeagueStats = new
                com.yasser.footballersmarket.playerstats.PlayerLeagueStats(1, 1, 2, 8.3);
        player.setLeagueStats(playerLeagueStats);
        playerRepository.save(player);

        PlayerDetailsResDto playerDetailsById = scoresService.getPlayerDetailsById(player.getSofascoreId());
        assertThat(playerDetailsById).isNotNull();
        assertThat(playerDetailsById.getLeagueStats()).isNotNull();
        assertThat(playerDetailsById.getLeagueStats().getRating()).isEqualTo(player.getLeagueStats().getRating());
        assertThat(playerDetailsById.getLeagueStats().getGoals()).isEqualTo(player.getLeagueStats().getGoals());
        assertThat(playerDetailsById.getName()).isEqualTo(player.getName());

        Player playerById = playerRepository.findPlayerById(1L);
        assertThat(playerById.getUpdatedBy()).isNull();
        assertThat(playerById.getLeagueStats()).isNotNull();
        assertThat(playerById.getLeagueStats().getGoals()).isEqualTo(player.getLeagueStats().getGoals());
    }
}