package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixtureSavingServiceTest {

    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private TeamRepository teamRepository;

    private FixtureSavingService savingService;

    @BeforeEach
    void setUp() {
        savingService = new FixtureSavingService(fixtureRepository, teamRepository);
    }

    @Test
    void shouldStoreEveryFixtureButFlagOnlyTheImportantOnes() {
        when(fixtureRepository.findAllById(any())).thenReturn(List.of());
        FixtureResponseDto fetched = new FixtureResponseDto(new Response[]{
                fixture(1L, 42L, 50L),     // arsenal v city: important
                fixture(2L, 60L, 61L)});   // two regular sides: stored, not important

        savingService.saveNewFixtures(List.of(fetched), Map.of(), Set.of(1L));

        List<Fixture> saved = captureSavedFixtures();
        assertThat(saved).hasSize(2);
        assertThat(byId(saved, 1L).isImportant()).isTrue();
        assertThat(byId(saved, 2L).isImportant()).isFalse();
    }

    @Test
    void shouldNotRewriteAFixtureThatIsAlreadyStored() {
        Fixture existing = new Fixture(1L, java.time.Instant.now(), 39L, "r", "FT", 42L, 50L, 2, 1, 42L);
        when(fixtureRepository.findAllById(any())).thenReturn(List.of(existing));
        FixtureResponseDto fetched = new FixtureResponseDto(new Response[]{
                fixture(1L, 42L, 50L), fixture(2L, 60L, 61L)});

        int inserted = savingService.saveNewFixtures(List.of(fetched), Map.of(), Set.of());

        assertThat(inserted).isEqualTo(1);
        assertThat(captureSavedFixtures()).extracting(Fixture::getId).containsExactly(2L);
    }

    private List<Fixture> captureSavedFixtures() {
        ArgumentCaptor<List<Fixture>> captor = ArgumentCaptor.forClass(List.class);
        verify(fixtureRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Fixture byId(List<Fixture> fixtures, Long id) {
        return fixtures.stream().filter(f -> f.getId().equals(id)).findFirst().orElseThrow();
    }

    private Response fixture(Long id, Long homeId, Long awayId) {
        return new Response(
                new Response.Fixture(id, "2026-11-15T20:00:00+03:00", 0L, new Response.Fixture.Status("NS")),
                new Response.League(39L, "League", "logo", "Regular Season - 12"),
                new Response.Teams(
                        new Response.Teams.Home(homeId, "home" + homeId, "logo", null),
                        new Response.Teams.Away(awayId, "away" + awayId, "logo", null)),
                new Response.Goals(null, null),
                null);
    }
}
