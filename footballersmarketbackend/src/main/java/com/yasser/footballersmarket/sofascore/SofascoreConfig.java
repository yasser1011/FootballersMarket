package com.yasser.footballersmarket.sofascore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SofascoreConfig {
    @Value("${sofascore-current-season-year}")
    private String currentSeasonYear;

    @Value("${sofascoreApiBaseUrl}")
    private String sofascoreApiBaseUrl;

    @Value("${sofascoreEventBaseUrl}")
    private String sofascoreEventBaseUrl;

    public String getCurrentSeasonYear() {
        return currentSeasonYear;
    }

    public String getSofascoreApiBaseUrl() {
        return sofascoreApiBaseUrl;
    }

    public String getSofascoreEventBaseUrl() {
        return sofascoreEventBaseUrl;
    }
}
