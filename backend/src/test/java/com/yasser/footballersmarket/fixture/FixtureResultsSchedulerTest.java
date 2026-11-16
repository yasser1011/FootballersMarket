package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.client.FixtureResultsClient;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixtureResultsSchedulerTest {

    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private FixtureResultsClient fixtureResultsClient;
    @Mock
    private FixtureResultsService fixtureResultsService;

    private FixtureResultsScheduler scheduler;

    @BeforeEach
    void setUp() throws InterruptedException {
        // spy so the real ten second pause between requests can be skipped
        scheduler = spy(new FixtureResultsScheduler(fixtureRepository, fixtureResultsClient, fixtureResultsService));
        // lenient: only the multi-batch test reaches a pause
        lenient().doNothing().when(scheduler).pauseBetweenRequests();
    }

    @Test
    void shouldDoNothingWhenNoFixtureIsDue() throws InterruptedException {
        // first call = the poll candidates, second = the aged-out check
        when(fixtureRepository.findPollable(any(), any(), any())).thenReturn(List.of(), List.of());

        scheduler.pollFixtureResults();

        verify(fixtureResultsClient, never()).fetchFixtureResults(anyList());
    }

    @Test
    void shouldSplitCandidatesIntoBatchesOfTwentyWithAPauseBetweenThem() throws InterruptedException {
        List<Fixture> candidates = fixtures(1, 45);
        when(fixtureRepository.findPollable(any(), any(), any())).thenReturn(candidates, List.of());
        when(fixtureResultsClient.fetchFixtureResults(anyList())).thenReturn(Map.of());

        scheduler.pollFixtureResults();

        ArgumentCaptor<List<Long>> batches = ArgumentCaptor.forClass(List.class);
        verify(fixtureResultsClient, times(3)).fetchFixtureResults(batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(20, 20, 5);
        // 3 requests -> 2 pauses, none before the first
        verify(scheduler, times(2)).pauseBetweenRequests();
    }

    @Test
    void shouldHandOneFixtureItsOwnResultAndSkipOnesTheApiDidNotReturn() throws InterruptedException {
        List<Fixture> candidates = fixtures(1, 3);
        Response result1 = result(1L);
        Response result3 = result(3L);
        when(fixtureRepository.findPollable(any(), any(), any())).thenReturn(candidates, List.of());
        when(fixtureResultsClient.fetchFixtureResults(anyList()))
                .thenReturn(Map.of(1L, result1, 3L, result3));   // fixture 2 missing

        scheduler.pollFixtureResults();

        verify(fixtureResultsService).applyResult(candidates.get(0), result1);
        verify(fixtureResultsService).applyResult(candidates.get(2), result3);
        verify(fixtureResultsService, times(2)).applyResult(any(), any());   // nothing for fixture 2
    }

    @Test
    void shouldKeepApplyingTheRestWhenOneFixtureFails() throws InterruptedException {
        List<Fixture> candidates = fixtures(1, 2);
        Response result1 = result(1L);
        Response result2 = result(2L);
        when(fixtureRepository.findPollable(any(), any(), any())).thenReturn(candidates, List.of());
        when(fixtureResultsClient.fetchFixtureResults(anyList())).thenReturn(Map.of(1L, result1, 2L, result2));
        doThrow(new RuntimeException("boom")).when(fixtureResultsService).applyResult(candidates.get(0), result1);

        scheduler.pollFixtureResults();

        verify(fixtureResultsService).applyResult(candidates.get(1), result2);
    }

    private List<Fixture> fixtures(long fromId, long toId) {
        return LongStream.rangeClosed(fromId, toId)
                .mapToObj(id -> new Fixture(id, Instant.now().minusSeconds(10_000), 39L, "r", "NS", 1L, 2L,
                        null, null, null))
                .collect(Collectors.toList());
    }

    private Response result(Long fixtureId) {
        return new Response(new Response.Fixture(fixtureId, null, null, null), null, null, null, null);
    }
}
