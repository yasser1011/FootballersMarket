package com.yasser.footballersmarket.fixture.client;

import com.yasser.footballersmarket.fixture.FixtureResultConstants;
import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.rapidapi.RapidApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FixtureResultsClient {
    private final Logger logger = LoggerFactory.getLogger(FixtureResultsClient.class);
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;

    public FixtureResultsClient(RestTemplate restTemplate, RapidApiConfig rapidApiConfig) {
        this.restTemplate = restTemplate;
        this.rapidApiConfig = rapidApiConfig;
    }

    // one request for up to MAX_IDS_PER_REQUEST fixtures. unlike the date-range query used by
    // discovery, this call carries the per-player minutes and match ratings, which is the whole
    // reason results are fetched by id. returns the results keyed by fixture id; ids the api did not
    // return are simply absent. empty on any failure, so one bad batch never aborts the poll
    public Map<Long, FixtureResponseDto.Response> fetchFixtureResults(List<Long> fixtureIds) {
        if (fixtureIds.isEmpty()) return Map.of();
        if (fixtureIds.size() > FixtureResultConstants.MAX_IDS_PER_REQUEST) {
            throw new IllegalArgumentException("at most " + FixtureResultConstants.MAX_IDS_PER_REQUEST
                    + " fixture ids per request, got " + fixtureIds.size());
        }

        String idsParam = fixtureIds.stream().map(String::valueOf).collect(Collectors.joining("-"));
        String fullUrl = rapidApiConfig.getRapidApiUrl() + rapidApiConfig.getRapidApiFixturesEndpoint()
                + "?ids=" + idsParam;
        logger.info("fetching fixture results url: {}", fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());

        try {
            ResponseEntity<FixtureResponseDto> res = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, new HttpEntity<Void>(headers), FixtureResponseDto.class);

            FixtureResponseDto body = res.getBody();
            if (body == null) {
                logger.error("fixture results: empty body for ids {}", idsParam);
                return Map.of();
            }
            // the api answers 200 even when it rejects the request
            if (body.errors() != null && body.errors().size() > 0) {
                logger.error("fixture results: api reported errors for ids {}: {}", idsParam, body.errors());
                return Map.of();
            }
            if (body.response() == null) return Map.of();

            Map<Long, FixtureResponseDto.Response> byId = new HashMap<>();
            for (FixtureResponseDto.Response result : body.response()) {
                if (result != null && result.fixture() != null && result.fixture().id() != null) {
                    byId.put(result.fixture().id(), result);
                }
            }
            return byId;
        } catch (RestClientException e) {
            logger.error("fixture results: fetch failed for ids {}: {}", idsParam, e.getMessage());
            return Map.of();
        }
    }
}
