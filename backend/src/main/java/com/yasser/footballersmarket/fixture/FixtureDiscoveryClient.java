package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.rapidapi.RapidApiConfig;
import com.yasser.footballersmarket.rapidapi.dto.PlayersApiResponse;
import com.yasser.footballersmarket.scores365.dto.FeaturedPlayersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class FixtureDiscoveryClient {
    private final Logger logger = LoggerFactory.getLogger(FixtureDiscoveryClient.class);
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;

    public FixtureDiscoveryClient(RestTemplate restTemplate, RapidApiConfig rapidApiConfig){
        this.restTemplate = restTemplate;
        this.rapidApiConfig = rapidApiConfig;
    }

    public FixtureResponseDto fetchFixtures(Long leagueId, LocalDate from, LocalDate to){
        String fullUrl = rapidApiConfig.getRapidApiUrl() + rapidApiConfig.getRapidApiFixturesEndpoint() +
                "?league=" + leagueId +"&season=" + rapidApiConfig.getRapidApiSeason() + "&from="+from+"&to=" + to;
        logger.info("fetching fixtures url: {}", fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<FixtureResponseDto> res = restTemplate.exchange(
                fullUrl, HttpMethod.GET, requestEntity, FixtureResponseDto.class);

        if(res.getBody() == null){
            logger.error("error in fetching fixtures data url: {}", fullUrl);
            throw new IllegalStateException("cannot fetch fixtures");
        }

        logger.info("URL: {} | Status: {} | list Length: {}",
                fullUrl,
                res.getStatusCode(),
                res.getBody().response().length);

        return res.getBody();
    }
}
