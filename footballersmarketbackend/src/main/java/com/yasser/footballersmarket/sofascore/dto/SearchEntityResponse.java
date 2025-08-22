package com.yasser.footballersmarket.sofascore.dto;

import java.util.List;

public class SearchEntityResponse {
    List<SearchPlayerEntity> results;

    public SearchEntityResponse(){}

    public SearchEntityResponse(List<SearchPlayerEntity> results) {
        this.results = results;
    }

    public List<SearchPlayerEntity> getResults() {
        return results;
    }

    public void setResults(List<SearchPlayerEntity> results) {
        this.results = results;
    }
}
