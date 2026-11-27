package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.fixture.FixtureResultConstants;
import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.player.PlayerService;
import com.yasser.footballersmarket.player.dto.PlayerDetailsResDto;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBought;
import com.yasser.footballersmarket.playerusercurrentbought.PlayerUserCurrentBoughtService;
import com.yasser.footballersmarket.pricing.PriceStrategy;
import com.yasser.footballersmarket.sofascore.SofascoreService;
import com.yasser.footballersmarket.transaction.dto.TransactionException;
import com.yasser.footballersmarket.transaction.dto.TransactionFailure;
import com.yasser.footballersmarket.transaction.dto.TransactionRequest;
import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// the transaction boundary of a buy/sell: the player is resolved (possibly over http) before any
// transaction opens, and only the db work runs inside one, with the 10s budget
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    private static final Long USER_ID = 7L;
    private static final Long PLAYER_ID = 1100L;
    private static final int BUY = 1;
    private static final int SELL = 2;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private SofascoreService sofascoreService;
    @Mock
    private PlayerUserCurrentBoughtService playerUserCurrentBoughtService;
    @Mock
    private UserService userService;
    @Mock
    private PriceStrategy priceStrategy;
    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, playerService, sofascoreService,
                playerUserCurrentBoughtService, userService, priceStrategy,
                fixtureRepository, transactionManager);
    }

    @Test
    void shouldRejectAWrongPriceWithoutOpeningATransaction() {
        givenResolvedPlayerPricedAt(900);

        assertThatThrownBy(() -> transactionService.makeTransaction(USER_ID, buyAt(800)))
                .isInstanceOf(TransactionException.class);
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    void shouldResolveThePlayerBeforeTheTransactionOpens() {
        givenResolvedPlayerPricedAt(800);
        givenABuyCanGoThrough();

        transactionService.makeTransaction(USER_ID, buyAt(800));

        // a 365 call while resolving must never hold the transaction's connection
        InOrder order = inOrder(playerService, transactionManager);
        order.verify(playerService).resolveForTransaction(PLAYER_ID, null);
        order.verify(transactionManager).getTransaction(any());
    }

    @Test
    void shouldBuyInsideOneTransactionWithTheTenSecondBudget() {
        givenResolvedPlayerPricedAt(800);
        User user = givenABuyCanGoThrough();

        transactionService.makeTransaction(USER_ID, buyAt(800));

        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertThat(definition.getValue().getTimeout()).isEqualTo(10);
        verify(transactionManager).commit(any());
        assertThat(user.getPoints()).isEqualTo(10000 - 800);
        verify(playerUserCurrentBoughtService).savePlayerUserCurrBoughtTransaction(any());
    }

    // ---- live match: no buy or sell while his club is playing ----

    @Test
    void shouldNotBuyAPlayerWhoseClubIsPlayingNow() {
        givenResolvedPlayerPricedAt(800);
        User user = givenABuyerWithAnEmptySquad();
        givenHisClubIsPlaying(true);

        assertThatThrownBy(() -> transactionService.makeTransaction(USER_ID, buyAt(800)))
                .isInstanceOf(TransactionException.class)
                .extracting(e -> ((TransactionException) e).getTransactionFailure())
                .isEqualTo(TransactionFailure.PLAYER_IN_LIVE_MATCH);
        assertThat(user.getPoints()).isEqualTo(10000);
        verify(playerUserCurrentBoughtService, never()).savePlayerUserCurrBoughtTransaction(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldNotSellAPlayerWhoseClubIsPlayingNow() {
        // a red card mid-match: selling at the pre-match price is the same exploit the other way
        givenResolvedPlayerPricedAt(800);
        User user = givenHeOwnsThePlayer();
        givenHisClubIsPlaying(true);

        assertThatThrownBy(() -> transactionService.makeTransaction(USER_ID, sellAt(800)))
                .isInstanceOf(TransactionException.class)
                .extracting(e -> ((TransactionException) e).getTransactionFailure())
                .isEqualTo(TransactionFailure.PLAYER_IN_LIVE_MATCH);
        assertThat(user.getPoints()).isEqualTo(10000);
        verify(playerUserCurrentBoughtService, never()).deletePlayerUserCurrBoughtTransaction(any(), any());
    }

    @Test
    void shouldSellAPlayerWhoseClubIsNotPlaying() {
        givenResolvedPlayerPricedAt(800);
        User user = givenHeOwnsThePlayer();
        givenHisClubIsPlaying(false);
        when(transactionRepository.save(any()))
                .thenReturn(new Transaction(1L, SELL, 800, LocalDateTime.now(), USER_ID, PLAYER_ID));

        transactionService.makeTransaction(USER_ID, sellAt(800));

        assertThat(user.getPoints()).isEqualTo(10000 + 800);
        verify(playerUserCurrentBoughtService).deletePlayerUserCurrBoughtTransaction(USER_ID, PLAYER_ID);
    }

    @Test
    void shouldCountOnlyMatchesThatKickedOffInTheLastThreeHoursAndAreNotOver() {
        givenResolvedPlayerPricedAt(800);
        givenABuyCanGoThrough();

        transactionService.makeTransaction(USER_ID, buyAt(800));

        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> earliestKickoff = ArgumentCaptor.forClass(Instant.class);
        verify(fixtureRepository).isPlayersClubPlayingNow(eq(PLAYER_ID), now.capture(),
                earliestKickoff.capture(), eq(FixtureResultConstants.EXCLUDED_FROM_RESULTS_POLL));
        assertThat(Duration.between(earliestKickoff.getValue(), now.getValue())).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void shouldNotCheckOurFixturesForAPlayerNotInOurDbYet() {
        // his club isn't in our fixtures, so there is nothing to look up
        PlayerDetailsResDto newPlayer = new PlayerDetailsResDto();
        newPlayer.setPrice(800);
        when(playerService.resolveForTransaction(null, 555)).thenReturn(newPlayer);
        when(playerService.convertPlayerDtoToPlayerEntity(newPlayer)).thenReturn(new Player());
        givenABuyCanGoThrough();

        transactionService.makeTransaction(USER_ID, new TransactionRequest(null, 555, BUY, 800));

        verifyNoInteractions(fixtureRepository);
        verify(playerService).savePlayer(any());
    }

    // ---- helpers ----

    private void givenHisClubIsPlaying(boolean playing) {
        when(fixtureRepository.isPlayersClubPlayingNow(eq(PLAYER_ID), any(), any(), any())).thenReturn(playing);
    }

    private User givenHeOwnsThePlayer() {
        User user = new User(USER_ID, "user1", "pass1", 10000);
        when(userService.findByIdForUpdate(USER_ID)).thenReturn(user);
        when(playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(USER_ID))
                .thenReturn(List.of(new PlayerUserCurrentBought(USER_ID, PLAYER_ID, 700)));
        return user;
    }

    private TransactionRequest sellAt(int price) {
        return new TransactionRequest(PLAYER_ID, null, SELL, price);
    }

    private void givenResolvedPlayerPricedAt(int price) {
        PlayerDetailsResDto player = new PlayerDetailsResDto();
        player.setId(PLAYER_ID);
        player.setPrice(price);
        when(playerService.resolveForTransaction(PLAYER_ID, null)).thenReturn(player);
    }

    private User givenABuyerWithAnEmptySquad() {
        User user = new User(USER_ID, "user1", "pass1", 10000);
        when(userService.findByIdForUpdate(USER_ID)).thenReturn(user);
        when(playerUserCurrentBoughtService.getUserBasicCurrBoughtPlayers(USER_ID)).thenReturn(List.of());
        return user;
    }

    private User givenABuyCanGoThrough() {
        User user = givenABuyerWithAnEmptySquad();
        when(transactionRepository.save(any()))
                .thenReturn(new Transaction(1L, BUY, 800, LocalDateTime.now(), USER_ID, PLAYER_ID));
        return user;
    }

    private TransactionRequest buyAt(int price) {
        return new TransactionRequest(PLAYER_ID, null, BUY, price);
    }
}
