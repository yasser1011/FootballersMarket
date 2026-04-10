package com.yasser.footballersmarket.player.dto;

import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;

import javax.persistence.CascadeType;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Transient;
import java.time.LocalDate;
import java.util.*;

public class PlayerResponseDto {
    private Long id;
    private Integer sofascoreId;
    private String name;
    private String nationality;
    private LocalDate dateOfBirth;
    private String position;
    private String rapidApiClub;
    private String sofascoreClub;
    private Boolean currentlyInjured;
    private Integer totalGoals;
    private Integer totalAssists;
    private Double avgRating;
    private Boolean areClubStatsUpdated;

    private PlayerLeagueStats leagueStats;
    private PlayerChampionsLeagueStats championsLeagueStats;

    private Integer price;
    private String photoUrl;
    private String clubPhotoUrl;
    private String updatedBy;

    private static final Map<String, String> NORMALIZATION_MAP = Map.of(
            "utd", "united",
            "man", "manchester",
            "munchen", "munich",
            "psg", "paris saint germain"
    );

    private static final Set<String> IGNORED_WORDS = Set.of(
            "fc", "cf", "club","u19", "u18", "u17", "u16"
    );

    public PlayerResponseDto(Long id, Integer sofascoreId, String name, String nationality,
                             LocalDate dateOfBirth, String position, String rapidApiClub,
                             String sofascoreClub, Boolean currentlyInjured,
                             Long leaguePlayerId,
                             Integer leagueNumOfGames, Integer leagueGoals, Integer leagueAssists,
                             Double leagueRating,
                             Long clPlayerId, Integer clNumOfGames, Integer clGoals,
                             Integer clAssists, Double clRating, Integer price, String photoUrl,
                             String clubPhotoUrl, String updatedBy) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.name = name;
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
        this.position = position;
        this.rapidApiClub = rapidApiClub;
        this.sofascoreClub = sofascoreClub;
        this.currentlyInjured = currentlyInjured;

        if(leaguePlayerId == null)
            this.leagueStats = null;
        else
            this.leagueStats = new PlayerLeagueStats(leagueGoals, leagueAssists, leagueNumOfGames, leagueRating);

        if(clPlayerId == null)
            this.championsLeagueStats = null;
        else
            this.championsLeagueStats = new PlayerChampionsLeagueStats(clNumOfGames, clGoals, clAssists, clRating);

        this.price = price;
        this.photoUrl = photoUrl;
        this.clubPhotoUrl = clubPhotoUrl;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public Integer getSofascoreId() {
        return sofascoreId;
    }

    public String getName() {
        return name;
    }

    public String getNationality() {
        return nationality;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPosition() {
        return position;
    }

    public String getRapidApiClub() {
        return rapidApiClub;
    }

    public String getSofascoreClub() {
        return sofascoreClub;
    }

    public Boolean getCurrentlyInjured() {
        return currentlyInjured;
    }

    public PlayerLeagueStats getLeagueStats() {
        return leagueStats;
    }

    public PlayerChampionsLeagueStats getChampionsLeagueStats() {
        return championsLeagueStats;
    }

    public Integer getPrice() {
        return price;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getClubPhotoUrl() {
        return clubPhotoUrl;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Double getAvgRating() {
        Double avgRating = 0.0;
        Double clRateW = 0.7;
        Double leagueRateW = 0.3;

        if(championsLeagueStats == null){
            double result = Math.round(leagueStats.getRating() * 100.0) / 100.0;
            return result;
        }
        if (leagueStats == null){
            double result = Math.round(championsLeagueStats.getRating() * 100.0) / 100.0;
            return result;
        }

        double result = leagueStats.getRating() * leagueRateW + championsLeagueStats.getRating() * clRateW;
        result = Math.round(result * 100.0) / 100.0;
        return result;
    }

    public Integer getTotalGoals() {
        int totalGoals = 0;
        if(leagueStats != null)
            totalGoals += leagueStats.getGoals();
        if (championsLeagueStats != null)
            totalGoals += championsLeagueStats.getGoals();

        return totalGoals;
    }

    public Integer getTotalAssists() {
        int totalAssists = 0;
        if(leagueStats != null)
            totalAssists += leagueStats.getAssists();
        if (championsLeagueStats != null)
            totalAssists += championsLeagueStats.getAssists();

        return totalAssists;
    }

    public Boolean getAreClubStatsUpdated() {
        if (rapidApiClub == null || sofascoreClub == null)
            return false;
        String normalized1 = normalizeClubName(rapidApiClub);
        String normalized2 = normalizeClubName(sofascoreClub);

        return normalized1.equals(normalized2);
    }

    private String normalizeClubName(String name) {
        // remove accents (München -> Munchen)
        name = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String[] words = name.toLowerCase().split(" ");

        List<String> normalizedWords = new ArrayList<>();

        for (String word : words) {
            if (IGNORED_WORDS.contains(word)) continue;

            if (NORMALIZATION_MAP.containsKey(word)) {
                String mapped = NORMALIZATION_MAP.get(word);
                normalizedWords.addAll(Arrays.asList(mapped.split(" ")));
            } else {
                normalizedWords.add(word);
            }
        }

        Collections.sort(normalizedWords);

        return String.join(" ", normalizedWords);
    }

    private boolean isClubNamesSimilar(String firstClub, String secondClub){
        String[] firstClubStrArr = firstClub.split(" ");
        String[] secondClubStrArr = secondClub.split(" ");


        int lengthToLoop = Math.min(Math.min(2, firstClubStrArr.length), Math.min(2, secondClubStrArr.length));

        for (int i = 0; i < lengthToLoop; i++){
            String wrdToCompare1 = firstClubStrArr[i].toLowerCase();
            String wrdToCompare2 = secondClubStrArr[i].toLowerCase();
            if (wrdToCompare1.equals("utd"))
                wrdToCompare1 = "united";
            if(wrdToCompare2.equals("utd"))
                wrdToCompare2 = "united";

            // 3 because of "man" word as "manchester"
            int subIdx1 = Math.min(3, wrdToCompare1.length());
            int subIdx2 = Math.min(3, wrdToCompare2.length());

            String subWrd1 = wrdToCompare1.substring(0, subIdx1);
            String subWrd2 = wrdToCompare2.substring(0, subIdx2);

            if (!subWrd1.equals(subWrd2)){
                return false;
            }
        }
        return true;
    }
}
