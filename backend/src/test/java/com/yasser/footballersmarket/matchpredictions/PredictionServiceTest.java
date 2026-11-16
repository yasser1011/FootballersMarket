package com.yasser.footballersmarket.matchpredictions;

import com.yasser.footballersmarket.fixture.Fixture;
import com.yasser.footballersmarket.fixture.FixtureRepository;
import com.yasser.footballersmarket.matchpredictions.dto.PredictionReqDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long FIXTURE_ID = 1557427L;
    private static final Long HOME_TEAM_ID = 42L;   // arsenal
    private static final Long AWAY_TEAM_ID = 50L;   // man city
    private static final Long OTHER_TEAM_ID = 33L;  // not in this fixture

    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private FixtureRepository fixtureRepository;

    private PredictionService predictionService;

    @BeforeEach
    void setUp() {
        predictionService = new PredictionService(predictionRepository, fixtureRepository);
    }

    @Test
    void shouldSavePredictedDrawForLeagueFixture() {
        givenFixtureKickingOffIn(Duration.ofHours(2));
        when(predictionRepository.findByUserIdAndFixtureId(USER_ID, FIXTURE_ID)).thenReturn(Optional.empty());

        // a level score with no winner is a predicted draw, which league fixtures allow
        predictionService.savePrediction(USER_ID, new PredictionReqDto(FIXTURE_ID, 1, 1, null));

        MatchPrediction saved = captureSaved();
        assertThat(saved.getHomeScore()).isEqualTo(1);
        assertThat(saved.getAwayScore()).isEqualTo(1);
        assertThat(saved.getWinnerTeamId()).isNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getFixtureId()).isEqualTo(FIXTURE_ID);
    }

    @Test
    void shouldUpdateExistingPredictionRatherThanCreateASecond() {
        givenFixtureKickingOffIn(Duration.ofHours(2));
        MatchPrediction existing = new MatchPrediction(FIXTURE_ID, USER_ID, 0, 0, null);
        when(predictionRepository.findByUserIdAndFixtureId(USER_ID, FIXTURE_ID)).thenReturn(Optional.of(existing));

        predictionService.savePrediction(USER_ID, new PredictionReqDto(FIXTURE_ID, 3, 1, HOME_TEAM_ID));

        MatchPrediction saved = captureSaved();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getHomeScore()).isEqualTo(3);
        assertThat(saved.getWinnerTeamId()).isEqualTo(HOME_TEAM_ID);
    }

    @Test
    void shouldRejectPredictionAfterKickoff() {
        givenFixtureKickingOffIn(Duration.ofHours(-1));

        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, 1, 0, HOME_TEAM_ID)))
                .isInstanceOf(PredictionClosedException.class);

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void shouldRejectWinnerThatIsNotPlayingInTheFixture() {
        givenFixtureKickingOffIn(Duration.ofHours(2));

        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, 2, 1, OTHER_TEAM_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a team in this fixture");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void shouldRejectScoreThatContradictsTheNamedWinner() {
        givenFixtureKickingOffIn(Duration.ofHours(2));

        // home named as winner, but the scoreline says the away team won
        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, 0, 2, HOME_TEAM_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void shouldRejectOnlyOneScoreBeingProvided() {
        givenFixtureKickingOffIn(Duration.ofHours(2));

        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, 2, null, HOME_TEAM_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both predicted scores or neither");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnEmptyPrediction() {
        givenFixtureKickingOffIn(Duration.ofHours(2));

        // no winner and no score would otherwise settle as a predicted draw and could win points
        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provide a predicted winner or a predicted score");

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void shouldAcceptAWinnerWithNoScore() {
        givenFixtureKickingOffIn(Duration.ofHours(2));
        when(predictionRepository.findByUserIdAndFixtureId(USER_ID, FIXTURE_ID)).thenReturn(Optional.empty());

        predictionService.savePrediction(USER_ID, new PredictionReqDto(FIXTURE_ID, null, null, HOME_TEAM_ID));

        MatchPrediction saved = captureSaved();
        assertThat(saved.getWinnerTeamId()).isEqualTo(HOME_TEAM_ID);
        assertThat(saved.getHomeScore()).isNull();
    }

    @Test
    void shouldRejectUnknownFixture() {
        when(fixtureRepository.findById(FIXTURE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, 1, 0, HOME_TEAM_ID)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(predictionRepository, never()).save(any());
    }

    @Test
    void shouldRejectAFixtureThatIsNotOpenForPredictions() {
        // stored for player stats, but not one of the tracked-team fixtures users predict on
        Fixture fixture = new Fixture(FIXTURE_ID, Instant.now().plus(Duration.ofHours(2)), 39L,
                "Regular Season - 12", "NS", HOME_TEAM_ID, AWAY_TEAM_ID, null, null, null);
        fixture.setImportant(false);
        when(fixtureRepository.findById(FIXTURE_ID)).thenReturn(Optional.of(fixture));

        assertThatThrownBy(() -> predictionService.savePrediction(USER_ID,
                new PredictionReqDto(FIXTURE_ID, 1, 0, HOME_TEAM_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not open");

        verify(predictionRepository, never()).save(any());
    }

    private void givenFixtureKickingOffIn(Duration untilKickoff) {
        Fixture fixture = new Fixture(FIXTURE_ID, Instant.now().plus(untilKickoff), 39L,
                "Regular Season - 12", "NS", HOME_TEAM_ID, AWAY_TEAM_ID, null, null, null);
        when(fixtureRepository.findById(FIXTURE_ID)).thenReturn(Optional.of(fixture));
    }

    private MatchPrediction captureSaved() {
        ArgumentCaptor<MatchPrediction> captor = ArgumentCaptor.forClass(MatchPrediction.class);
        verify(predictionRepository).save(captor.capture());
        return captor.getValue();
    }
}
