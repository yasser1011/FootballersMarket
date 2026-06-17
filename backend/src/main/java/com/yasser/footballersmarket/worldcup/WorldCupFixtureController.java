package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.worldcup.dto.WorldCupFixtureResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// public world cup fixtures view: yesterday + today + tomorrow
@RestController
@RequestMapping("/api/wc/fixtures")
public class WorldCupFixtureController {
    private final WorldCupFixtureService fixtureService;

    public WorldCupFixtureController(WorldCupFixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping
    public List<WorldCupFixtureResponse> recentAndUpcoming() {
        return fixtureService.getRecentAndUpcomingFixtures();
    }
}
