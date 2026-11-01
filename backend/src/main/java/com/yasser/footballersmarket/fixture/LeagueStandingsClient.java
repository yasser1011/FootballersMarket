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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeagueStandingsClient {
    private final Logger logger = LoggerFactory.getLogger(LeagueStandingsClient.class);
    private final RestTemplate restTemplate;
    private final RapidApiConfig rapidApiConfig;

    public LeagueStandingsClient(RestTemplate restTemplate, RapidApiConfig rapidApiConfig){
        this.restTemplate = restTemplate;
        this.rapidApiConfig = rapidApiConfig;
    }

    public List<Long> fetchOrderedTeamIds(Long leagueId){
        LeagueStandingsDto dto;
        try {
            dto = fetchLeagueStandings(leagueId);
        } catch (RestClientException | IllegalStateException e) {
            logger.error("standings: fetch failed for league {}: {}", leagueId, e.getMessage());
            return List.of();
        }

        if (dto.response() == null || dto.response().length == 0) {
            logger.warn("standings: empty response for league {}", leagueId);
            return List.of();
        }
        LeagueStandingsDto.Response.League league = dto.response()[0].league();
        if (league == null || league.standings() == null || league.standings().length == 0) {
            logger.warn("standings: no table for league {}", leagueId);
            return List.of();
        }
        LeagueStandingsDto.Response.League.Standing[] table = league.standings()[0];
        if (table == null) {
            logger.warn("standings: null table for league {}", leagueId);
            return List.of();
        }

        return Arrays.stream(table)
                .filter(standing -> standing.team() != null && standing.team().id() != null)
                .map(standing -> standing.team().id())
                .collect(Collectors.toList());
    }

    private LeagueStandingsDto fetchLeagueStandings(Long leagueId) {
        // https://v3.football.api-sports.io/standings?league=39&season=2026
        String fullUrl = rapidApiConfig.getRapidApiUrl() + rapidApiConfig.getRapidApiLeagueStandingsEndpoint() +
                "?league=" + leagueId + "&season=" + rapidApiConfig.getRapidApiSeason();
        logger.info("fetching standings url: {}", fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiConfig.getRapidApiHost());
        headers.set("x-rapidapi-key", rapidApiConfig.getRapidApiKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<LeagueStandingsDto> res = restTemplate.exchange(
                fullUrl, HttpMethod.GET, requestEntity, LeagueStandingsDto.class);

        if (res.getBody() == null) {
            logger.error("error in fetching league standings url: {}", fullUrl);
            throw new IllegalStateException("cannot fetch standings");
        }

        return res.getBody();
    }

}
