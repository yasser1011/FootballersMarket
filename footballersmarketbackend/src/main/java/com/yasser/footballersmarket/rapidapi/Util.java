package com.yasser.footballersmarket.rapidapi;

import com.yasser.footballersmarket.player.dto.PlayerExternalServiceBasicDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

@Component
public class Util {
    public String determinePlayerSchedulerMultipleRecordsClub(String[] clubList, String externalServiceTeamName) {
        if (clubList.length < 1) return "";
        if (externalServiceTeamName == null || externalServiceTeamName.isEmpty()) return clubList[0];
        for (String clubName : clubList) {
            String s = clubName.toLowerCase();
            String s1 = externalServiceTeamName.toLowerCase();
            if (s1.contains(s) || s.contains(s1)){
                return clubName;
            }
        }
        return clubList[0];
    }

    public <T> HashMap<T, Integer> getPlayerIdsInHashmap(List<T> playerIds){
        HashMap<T, Integer> playerIdsHashmap = new HashMap<>();
        playerIds.forEach(id -> {
            playerIdsHashmap.put(id, 1);
        });

        return playerIdsHashmap;
    }

    public HashMap<Long, PlayerExternalServiceBasicDetails> getPlayerIdsWithBasicExternalServiceDetailsInHashmap(
            List<PlayerExternalServiceBasicDetails> playerIds){
        HashMap<Long, PlayerExternalServiceBasicDetails> playerIdsHashmap = new HashMap<>();
        playerIds.forEach(playerDet -> {
            playerIdsHashmap.put(playerDet.getId(), playerDet);
        });

        return playerIdsHashmap;
    }

    public String getFirstTwoWordsFromName(String playerName){
        // Use StringTokenizer to split the string into words
        StringTokenizer tokenizer = new StringTokenizer(playerName);

        // Check if there are at least two words
        if (tokenizer.countTokens() >= 2 && !playerName.contains(".")) {
            // Extract the first two words
            String firstTwoWords = tokenizer.nextToken() + " " + tokenizer.nextToken();
            return firstTwoWords;
        } else {
            return playerName;
        }
    }

    public boolean isCurrDateEndOfTransferMarket(String winterTransferMarketEndDateStr,
                                                  String summerTransferMarketEndDateStr){
        String[] winterTransferMarketEndDateStrSplit = winterTransferMarketEndDateStr.split("-");
        String[] summerTransferMarketEndDateStrSplit = summerTransferMarketEndDateStr.split("-");
        LocalDate winterTransferMarketEndDate = LocalDate.of(LocalDate.now().getYear()
                , Integer.parseInt(winterTransferMarketEndDateStrSplit[0]),
                Integer.parseInt(winterTransferMarketEndDateStrSplit[1]));
        LocalDate summerTransferMarketEndDate = LocalDate.of(LocalDate.now().getYear()
                , Integer.parseInt(summerTransferMarketEndDateStrSplit[0]),
                Integer.parseInt(summerTransferMarketEndDateStrSplit[1]));

        return LocalDate.now().isEqual(summerTransferMarketEndDate) ||
                LocalDate.now().isEqual(winterTransferMarketEndDate);
    }
}
