package com.yasser.footballersmarket.player.dto;

public class SearchPlayerRequest {
    private String name;

    public SearchPlayerRequest(){}

    public SearchPlayerRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
