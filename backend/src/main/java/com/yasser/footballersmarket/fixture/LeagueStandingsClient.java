package com.yasser.footballersmarket.fixture;

import com.yasser.footballersmarket.fixture.dto.FixtureResponseDto;
import com.yasser.footballersmarket.fixture.dto.LeagueStandingsDto;
import com.yasser.footballersmarket.rapidapi.RapidApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
public class LeagueStandingsClient {
    private final Logger logger = LoggerFactory.getLogger(LeagueStandingsClient.class);
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;

    public LeagueStandingsClient(RestTemplate restTemplate, RapidApiConfig rapidApiConfig){
        this.restTemplate = restTemplate;
        this.rapidApiConfig = rapidApiConfig;
    }

    public LeagueStandingsDto fetchLeagueStandings(Long leagueId){
        // https://v3.football.api-sports.io/standings?league=39&season=2026
        String fullUrl = rapidApiConfig.getRapidApiUrl() + rapidApiConfig.getRapidApiLeagueStandingsEndpoint() +
                "?league=" + leagueId +"&season=" + rapidApiConfig.getRapidApiSeason();
        logger.info("fetching standings url: {}", fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<LeagueStandingsDto> res = restTemplate.exchange(
                fullUrl, HttpMethod.GET, requestEntity, LeagueStandingsDto.class);

        if(res.getBody() == null){
            logger.error("error in fetching league standings url: {}", fullUrl);
            throw new IllegalStateException("cannot fetch standings");
        }

        return res.getBody();
    }
}
