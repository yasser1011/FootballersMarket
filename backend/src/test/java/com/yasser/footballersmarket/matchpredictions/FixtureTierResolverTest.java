package com.yasser.footballersmarket.matchpredictions;

import com.yasser.footballersmarket.fixture.tournament.TournamentCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.yasser.footballersmarket.fixture.*;

import java.time.Instant;
import java.util.OptionalInt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixtureTierResolverTest {

    @Mock
    TournamentCatalog tournamentCatalog;

    FixtureTierResolver fixtureTierResolver;

    @BeforeEach
    void setUp(){
        fixtureTierResolver = new FixtureTierResolver(tournamentCatalog);
    }

    @Test
    void shouldReturnHigherFixtureWeightWhenTwoCloserTeamsPlay() {
        // need to test tracked vs tracked give more weight than top vs regular
        Long trackedTeamId1 = 50L;
        Long trackedTeamId2 = 51L;
        Long topTeamId = 25L;
        Long regularTeamId = 26L;

        when(tournamentCatalog.isTrackedTeam(trackedTeamId1)).thenReturn(true);
        when(tournamentCatalog.isTrackedTeam(trackedTeamId2)).thenReturn(true);
        when(tournamentCatalog.isTrackedTeam(regularTeamId)).thenReturn(false);

        when(tournamentCatalog.leagueSize(3L)).thenReturn(OptionalInt.of(20));

        Fixture trackedVsTrackedFixture = new Fixture(500L, Instant.now(), 3L, "", "", trackedTeamId1, trackedTeamId2,
                null, null, null);
        trackedVsTrackedFixture.setHomeTeamPosition(5);
        trackedVsTrackedFixture.setAwayTeamPosition(6);

        Fixture topVsRegularFixture = new Fixture(501L, Instant.now(), 3L, "", "", topTeamId, regularTeamId,
                null, null, null);
        topVsRegularFixture.setHomeTeamPosition(2);
        topVsRegularFixture.setAwayTeamPosition(8);


        FixtureTierResolver.FixtureWeighting trackedVsTrackedWeight = fixtureTierResolver.resolve(trackedVsTrackedFixture);
        FixtureTierResolver.FixtureWeighting topVsRegularWeight = fixtureTierResolver.resolve(topVsRegularFixture);

        assertThat(trackedVsTrackedWeight.tier()).isGreaterThan(topVsRegularWeight.tier());
        assertThat(topVsRegularWeight.gapNorm()).isGreaterThan(trackedVsTrackedWeight.gapNorm());
    }
}