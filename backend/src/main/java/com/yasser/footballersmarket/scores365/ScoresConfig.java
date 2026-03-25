package com.yasser.footballersmarket.scores365;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoresConfig {
    @Value("${scoresApiBaseUrl}")
    private String scoresApiBaseUrl;
    @Value("${scoresImageBaseUrl}")
    private String scoresImageBaseUrl;
    @Value("${scoresPlayersImageEndPoint}")
    private String scoresPlayersImageEndPoint;
    @Value("${scoresClubsImageEndPoint}")
    private String scoresClubsImageEndPoint;

    public String getScoresApiBaseUrl() {
        return scoresApiBaseUrl;
    }

    public String getScoresImageBaseUrl() {
        return scoresImageBaseUrl;
    }

    public String getScoresPlayersImageEndPoint() {
        return scoresPlayersImageEndPoint;
    }

    public String getScoresClubsImageEndPoint() {
        return scoresClubsImageEndPoint;
    }
}
