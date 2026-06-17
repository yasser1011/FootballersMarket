package com.yasser.footballersmarket.worldcup;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// public list of world cup teams (id, name, logo, fifa rank) for the nationality filter dropdown
@RestController
@RequestMapping("/api/wc/teams")
public class WorldCupTeamController {
    private final WorldCupTeamRepository teamRepository;

    public WorldCupTeamController(WorldCupTeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @GetMapping
    public List<WorldCupTeam> getTeams() {
        return teamRepository.findAll(Sort.by("name"));
    }
}
