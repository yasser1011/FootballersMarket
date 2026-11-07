package com.yasser.footballersmarket.fixture.client;

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

@Service
public class FixtureResultsClient {
    private final Logger logger = LoggerFactory.getLogger(FixtureResultsClient.class);
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;

    public FixtureResultsClient(RestTemplate restTemplate, RapidApiConfig rapidApiConfig) {
        this.restTemplate = restTemplate;
        this.rapidApiConfig = rapidApiConfig;
    }

    // single-fixture call. unlike the date-range query used by discovery, this one carries the
    // per-player match ratings, which is the whole reason results are polled one fixture at a time.
    // null on any failure or empty response, so one bad fixture never aborts the polling batch
    public FixtureResponseDto.Response fetchFixtureResult(Long fixtureId) {
        String fullUrl = rapidApiConfig.getRapidApiUrl() + rapidApiConfig.getRapidApiFixturesEndpoint()
                + "?id=" + fixtureId;
        logger.info("fetching fixture result url: {}", fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());

        try {
            ResponseEntity<FixtureResponseDto> res = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, new HttpEntity<Void>(headers), FixtureResponseDto.class);

            FixtureResponseDto body = res.getBody();
            if (body == null || body.response() == null || body.response().length == 0) {
                logger.error("fixture result: empty response for fixture {}", fixtureId);
                return null;
            }
            return body.response()[0];
        } catch (RestClientException e) {
            logger.error("fixture result: fetch failed for fixture {}: {}", fixtureId, e.getMessage());
            return null;
        }
    }
}
