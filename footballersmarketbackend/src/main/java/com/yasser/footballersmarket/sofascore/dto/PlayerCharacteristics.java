package com.yasser.footballersmarket.sofascore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class PlayerCharacteristics {
    private List<Integer> positives;
    private List<Integer> negatives;

    public PlayerCharacteristics(){}

    @JsonProperty("positive")
    private void getPlayerPositives(List<Map<String,Object>> positiveList) {
        positiveList.forEach(characterObj ->{
            positives.add((Integer) characterObj.get("type"));
        });

    }

    @JsonProperty("negative")
    private void getPlayerNegatives(List<Map<String,Object>> negativeList) {
        negativeList.forEach(characterObj ->{
            negatives.add((Integer) characterObj.get("type"));
        });

    }

    public List<Integer> getPositives() {
        return positives;
    }

    public List<Integer> getNegatives() {
        return negatives;
    }
}
