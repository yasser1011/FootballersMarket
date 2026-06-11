package com.yasser.footballersmarket.worldcup;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "world_cup_team")
public class WorldCupTeam {
    // rapid api team id, assigned from scrap data (no generation)
    @Id
    private Long id;
    private String name;
    private Integer fifaRank;
    private String logoUrl;

    public WorldCupTeam() {}

    public WorldCupTeam(Long id, String name, Integer fifaRank, String logoUrl) {
        this.id = id;
        this.name = name;
        this.fifaRank = fifaRank;
        this.logoUrl = logoUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getFifaRank() {
        return fifaRank;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
