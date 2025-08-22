package com.yasser.footballersmarket.scores365.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class PlayerSearchEntityDto {
    private Integer id;
    private String name;
    private String clubName;
    private Integer clubId;
    private Integer sportId;

    public PlayerSearchEntityDto() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClubName() {
        return clubName;
    }

    public Integer getClubId() {
        return clubId;
    }

    public Integer getSportId() {
        return sportId;
    }
}
