package com.yasser.footballersmarket.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yasser.footballersmarket.fixture.client.FixtureResultsClient;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto.Response;
import com.yasser.footballersmarket.rapidapi.RapidApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FixtureResultsClientTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RapidApiConfig rapidApiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FixtureResultsClient client;

    @BeforeEach
    void setUp() {
        lenient().when(rapidApiConfig.getRapidApiUrl()).thenReturn("https://api.test");
        lenient().when(rapidApiConfig.getRapidApiFixturesEndpoint()).thenReturn("/fixtures");
        client = new FixtureResultsClient(restTemplate, rapidApiConfig);
    }

    @Test
    void shouldJoinTheIdsIntoOneRequestAndKeyTheResultsById() {
        FixtureResponseDto body = new FixtureResponseDto(new Response[]{result(101L), result(102L)});
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(FixtureResponseDto.class))).thenReturn(ResponseEntity.ok(body));

        Map<Long, Response> results = client.fetchFixtureResults(List.of(101L, 102L, 103L));

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(url.capture(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(FixtureResponseDto.class));
        assertThat(url.getValue()).isEqualTo("https://api.test/fixtures?ids=101-102-103");
        // 103 was asked for but not returned -> simply absent
        assertThat(results).containsOnlyKeys(101L, 102L);
    }

    @Test
    void shouldReturnNothingWhenTheApiAnswers200WithErrors() throws Exception {
        FixtureResponseDto body = new FixtureResponseDto(
                objectMapper.readTree("{\"ids\": \"too many ids\"}"), new Response[]{result(101L)});
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(FixtureResponseDto.class))).thenReturn(ResponseEntity.ok(body));

        assertThat(client.fetchFixtureResults(List.of(101L))).isEmpty();
    }

    @Test
    void shouldReturnNothingWhenTheCallFails() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(FixtureResponseDto.class))).thenThrow(new RestClientException("timeout"));

        assertThat(client.fetchFixtureResults(List.of(101L))).isEmpty();
    }

    @Test
    void shouldNotCallTheApiForNoIds() {
        assertThat(client.fetchFixtureResults(List.of())).isEmpty();

        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class),
                eq(FixtureResponseDto.class));
    }

    @Test
    void shouldRefuseMoreIdsThanTheApiAccepts() {
        List<Long> tooMany = LongStream.rangeClosed(1, FixtureResultConstants.MAX_IDS_PER_REQUEST + 1)
                .boxed().toList();

        assertThatThrownBy(() -> client.fetchFixtureResults(tooMany))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Response result(Long fixtureId) {
        return new Response(new Response.Fixture(fixtureId, null, null, null), null, null, null, null);
    }
}
