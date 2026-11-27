package com.yasser.footballersmarket.playerusercurrentbought;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// the squad lists each owned player through the same refresh and price as his details page and
// the rankings (what that refresh does per player case is PlayerServiceDetailsTest's)
@ExtendWith(MockitoExtension.class)
class PlayerUserCurrentBoughtServiceTest {
    private static final Long USER_ID = 7L;

    @Mock
    private PlayerUserCurrentBoughtRepository repository;
    @Mock
    private PlayerService playerService;

    private PlayerUserCurrentBoughtService service;

    @BeforeEach
    void setUp() {
        service = new PlayerUserCurrentBoughtService(repository, playerService);
    }

    @Test
    void shouldListEachOwnedPlayerAtHisRefreshedPriceMostExpensiveFirst() {
        Player cheap = new Player();
        Player dear = new Player();
        when(repository.getUserCurrBoughtPlayers(USER_ID)).thenReturn(List.of(
                new PlayerUserCurrentBought(1L, 500, USER_ID, 10L, cheap, LocalDateTime.now()),
                new PlayerUserCurrentBought(2L, 700, USER_ID, 20L, dear, LocalDateTime.now())));
        givenListedAt(cheap, 10L, 600);
        givenListedAt(dear, 20L, 1300);

        List<PlayerUserCurrentBoughtDto> squad = service.getUserBoughtPlayers(USER_ID);

        assertThat(squad).extracting(PlayerUserCurrentBoughtDto::getId).containsExactly(20L, 10L);
        assertThat(squad).extracting(PlayerUserCurrentBoughtDto::getPrice).containsExactly(1300, 600);
        // what he was bought at is kept beside today's price
        assertThat(squad).extracting(PlayerUserCurrentBoughtDto::getBuyPrice).containsExactly(700, 500);
    }

    private void givenListedAt(Player entity, Long id, int price) {
        PlayerDetailsResDto stored = new PlayerDetailsResDto();
        PlayerDetailsResDto refreshed = new PlayerDetailsResDto();
        refreshed.setId(id);
        refreshed.setPrice(price);
        when(playerService.convertPlayerEntityToPlayerDto(entity)).thenReturn(stored);
        when(playerService.refreshedForListing(stored)).thenReturn(refreshed);
    }
}
