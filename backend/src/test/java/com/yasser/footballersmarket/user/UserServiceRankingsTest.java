package com.yasser.footballersmarket.user;

import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// how the rankings value each owned player: a fixture player at the price the sql computed, an
// externally refreshed one at the price of his stored data refreshed -- the same price his details
// page and his squad show (the refresh itself, and 365 being down, are PlayerServiceDetailsTest's)
@ExtendWith(MockitoExtension.class)
class UserServiceRankingsTest {
    private static final long USER_ID = 7L;
    private static final long PLAYER_ID = 1100L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private SofascoreService sofascoreService;
    @Mock
    private PlayerService playerService;
    @Mock
    private PriceStrategy priceStrategy;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, sofascoreService, playerService, priceStrategy);
    }

    @Test
    void shouldValueAFixturePlayerAtTheSqlPrice() throws Exception {
        when(userRepository.getAllUsersScore()).thenReturn(List.<Object[]>of(ownedPlayerRow("fixture", 0, 900)));

        List<UserResponse> rankings = userService.getAllUsersScores();

        assertThat(rankings.get(0).getPoints()).isEqualTo(1000 + 900);
        verify(playerService, never()).refreshedForListing(any());
    }

    @Test
    void shouldValueAnExternalPlayerAtHisRefreshedStoredPrice() throws Exception {
        when(userRepository.getAllUsersScore()).thenReturn(List.<Object[]>of(ownedPlayerRow("sofascore", 0, 900)));
        PlayerDetailsResDto stored = new PlayerDetailsResDto();
        PlayerDetailsResDto refreshed = new PlayerDetailsResDto();
        refreshed.setPrice(1250);
        when(playerService.getPlayerDetailsByInternalId(PLAYER_ID)).thenReturn(stored);
        when(playerService.refreshedForListing(stored)).thenReturn(refreshed);

        List<UserResponse> rankings = userService.getAllUsersScores();

        // not the sql's 900, and not a price rebuilt from 365 alone
        assertThat(rankings.get(0).getPoints()).isEqualTo(1000 + 1250);
        verify(playerService, never()).convertExternalToPlayerDto(any(), any());
    }

    // the club-mode ranking row: [0] player id, [1] user id, [2] username, [3] points,
    // [4] external id, [5] updatedBy, [8] sql price, [9] missed matches
    private Object[] ownedPlayerRow(String updatedBy, int missedMatches, int sqlPrice) {
        Object[] row = new Object[10];
        row[0] = BigInteger.valueOf(PLAYER_ID);
        row[1] = BigInteger.valueOf(USER_ID);
        row[2] = "user1";
        row[3] = 1000;
        row[4] = 65760;
        row[5] = updatedBy;
        row[8] = BigDecimal.valueOf(sqlPrice);
        row[9] = missedMatches;
        return row;
    }
}
