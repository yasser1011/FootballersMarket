package com.yasser.footballersmarket.featuredplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yasser.footballersmarket.scores365.ScoresService;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.testcotainer.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeaturedPlayerServiceTest extends BaseIntegrationTest {
    // real repo but can override method to mock results
    @SpyBean
    private FeaturedPlayerRepository featuredPlayerRepository;
    @Autowired
    private FeaturedPlayerService featuredPlayerService;
    @MockBean
    private ScoresService scoresService;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        featuredPlayerRepository.deleteAll();
    }

    @Test
    void shouldRaiseExceptionAndReturnDbDataIfThereWasDuplicateDatabasePlayerInsertion(){
        // 1- add players to db normally
        // 2- run method again and let repo.find return empty at first time
        // 3- exception raised then return what's in the db
        // mockito does not call the real method first it just maps the behavior directly
        // works best for spies
        FeaturedPlayer entity = new FeaturedPlayer("test name", 1,
                "5.5", 1, 1, "home team", "away team");
        featuredPlayerRepository.save(entity);
        List<FeaturedPlayer> dbSnapshot = featuredPlayerRepository.findByDate(LocalDate.now());

        doReturn(Collections.emptyList())
                .doReturn(dbSnapshot)
                .when(featuredPlayerRepository).findByDate(any(LocalDate.class));

        FeaturedPlayer externalSrvcFeaturedPlayer1 = new FeaturedPlayer("player10", 10,
                "", 1, 10,"5.5", "test");
        FeaturedPlayer externalSrvcFeaturedPlayer2 = new FeaturedPlayer("player11", 11,
                "", 2,10,"5.5", "test");
        FeaturedPlayersResponse trendingPlayersResponseSof = new FeaturedPlayersResponse();
        trendingPlayersResponseSof.setFeaturedPlayers(List.of(externalSrvcFeaturedPlayer1, externalSrvcFeaturedPlayer2));
        when(scoresService.getFeaturedPlayers()).thenReturn(trendingPlayersResponseSof);

        FeaturedPlayersResponse featuredPlayers = featuredPlayerService.getFeaturedPlayers();
        assertThat(featuredPlayers.getFeaturedPlayers()).isNotNull();
        assertThat(featuredPlayers.getFeaturedPlayers().size()).isEqualTo(1);
        assertThat(featuredPlayers.getFeaturedPlayers().get(0).getPlayerName()).isEqualTo(entity.getPlayerName());
    }

}