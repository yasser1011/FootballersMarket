package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.rapidapi.RapidApiConfig;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse;
import com.yasser.footballersmarket.worldcup.dto.FixtureResultApiResponse.FixtureResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

// fetches world cup fixtures from api-football's /fixtures endpoint (single fixture result with
// player ratings, or a date range), reusing the same rapid host/key headers as the nightly scheduler
@Component
public class WorldCupResultsClient {
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;
    private final Logger logger = LoggerFactory.getLogger(WorldCupResultsClient.class);

    public WorldCupResultsClient(RestTemplate restTemplate, RapidApiConfig rapidApiConfig) {
        this.restTemplate = restTemplate;
        this.rapidApiConfig = rapidApiConfig;
    }

    // returns null on any error or empty response so one bad fixture never aborts the polling batch
    public FixtureResult fetchFixtureResult(Long fixtureId) {
        String url = rapidApiConfig.getRapidApiUrl() + "/fixtures?id=" + fixtureId;
        List<FixtureResult> results = fetch(url, "fixture " + fixtureId);
        return results.isEmpty() ? null : results.get(0);
    }

    // all world cup fixtures kicking off in [from, to] (inclusive dates). used to (re)build rows for
    // fixtures not seeded yet, e.g. knockout matches. empty list on any error.
    public List<FixtureResult> fetchFixturesByDateRange(LocalDate from, LocalDate to) {
        String url = rapidApiConfig.getRapidApiUrl() + "/fixtures?league=" + WorldCupConstants.WC_RAPID_LEAGUE_ID
                + "&season=" + WorldCupConstants.WC_RAPID_SEASON + "&from=" + from + "&to=" + to;
        return fetch(url, "range " + from + ".." + to);
    }

    private List<FixtureResult> fetch(String url, String what) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());
        try {
            ResponseEntity<FixtureResultApiResponse> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), FixtureResultApiResponse.class);
            FixtureResultApiResponse body = res.getBody();
            if (body == null || body.response() == null) {
                logger.error("world cup fixtures: empty response for {}", what);
                return Collections.emptyList();
            }
            return body.response();
        } catch (Exception e) {
            logger.error("world cup fixtures: fetch failed for {}: {}", what, e.getMessage());
            return Collections.emptyList();
        }
    }
}
