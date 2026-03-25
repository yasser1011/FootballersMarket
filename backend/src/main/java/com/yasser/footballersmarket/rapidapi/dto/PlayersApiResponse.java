package com.yasser.footballersmarket.rapidapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasser.footballersmarket.rapidapi.dto.PlayerResponse;

import java.util.List;
import java.util.Map;

public class PlayersApiResponse {
    private List<PlayerResponse> response;
    private Integer totalNoOfPages;

    public PlayersApiResponse(){}

    public PlayersApiResponse(List<PlayerResponse> response) {
        this.response = response;
    }

    public PlayersApiResponse(List<PlayerResponse> response, Integer totalNoOfPages) {
        this.response = response;
        this.totalNoOfPages = totalNoOfPages;
    }

    @JsonProperty("paging")
    private void getPages(Map<String, Object> pagingObj) {
        this.totalNoOfPages = (Integer) pagingObj.get("total");
    }

    public List<PlayerResponse> getResponse() {
        return response;
    }

    public void setResponse(List<PlayerResponse> response) {
        this.response = response;
    }

    public Integer getTotalNoOfPages() {
        return totalNoOfPages;
    }
}
