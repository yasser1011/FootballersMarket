package com.yasser.footballersmarket.player.dto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

public class PlayerDetailsResDto {
    private Long id;
    private Integer externalServicePlayerId;
    private String name;
    private String nationality;
    private LocalDate dateOfBirth;
    private String position;
    private String rapidApiClub;
    private String externalServicePlayerClub;
    private Boolean currentlyInjured;
    private Integer totalGoals;
    private Integer totalAssists;
    private Double avgRating;
    private Boolean areClubStatsUpdated;
    private PlayerLeagueStats leagueStats;
    private Integer price;
    // world cup pricing inputs, carried so the dto can be repriced after its stats
    // mutate; null for non-participants. not exposed in the api response
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Double worldCupBaseRating;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Double worldCupRating;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer worldCupGames;
    // tournament stats (rating/goals/assists/games + the national team for its flag & name),
    // exposed like leagueStats; null for non-participants
    private PlayerWorldCupStats worldCupStats;

    private String photoUrl;
    private String clubPhotoUrl;
    private String updatedBy;
    private List<PlayerRecentMatch> recentMatches;

    private static final Map<String, String> NORMALIZATION_MAP = Map.of(
            "utd", "united",
            "man", "manchester",
            "munchen", "munich",
            "psg", "paris saint germain"
    );

    private static final Set<String> IGNORED_WORDS = Set.of(
            "fc", "cf", "club","u19", "u18", "u17", "u16"
    );

    public PlayerDetailsResDto(){}

    public List<PlayerRecentMatch> getRecentMatches() {
        return recentMatches;
    }


    public Double getAvgRating() {
        if(leagueStats == null) return 0.0;
        Double avgRating = 0.0;
        Double clRateW = 0.7;
        Double leagueRateW = 0.3;

        double result = leagueStats.getRating(); // * leagueRateW;
        result = Math.round(result * 100.0) / 100.0;
        return result;
    }
    public Integer getTotalGoals() {
        int totalGoals = 0;
        if(leagueStats != null)
            totalGoals += leagueStats.getGoals();

        return totalGoals;
    }

    public Integer getTotalAssists() {
        int totalAssists = 0;
        if(leagueStats != null)
            totalAssists += leagueStats.getAssists();

        return totalAssists;
    }

    // computed by the active PriceStrategy and set during dto conversion;
    // the dto no longer knows any pricing formula
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Double getWorldCupBaseRating() {
        return worldCupBaseRating;
    }

    public void setWorldCupBaseRating(Double worldCupBaseRating) {
        this.worldCupBaseRating = worldCupBaseRating;
    }

    public Double getWorldCupRating() {
        return worldCupRating;
    }

    public void setWorldCupRating(Double worldCupRating) {
        this.worldCupRating = worldCupRating;
    }

    public Integer getWorldCupGames() {
        return worldCupGames;
    }

    public void setWorldCupGames(Integer worldCupGames) {
        this.worldCupGames = worldCupGames;
    }

    public PlayerWorldCupStats getWorldCupStats() {
        return worldCupStats;
    }

    public void setWorldCupStats(PlayerWorldCupStats worldCupStats) {
        this.worldCupStats = worldCupStats;
    }

    public Boolean getAreClubStatsUpdated() {
        if (rapidApiClub == null || externalServicePlayerClub == null)
            return false;
        String normalized1 = normalizeClubName(rapidApiClub);
        String normalized2 = normalizeClubName(externalServicePlayerClub);

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

    private Boolean areClubNamesSimilar(String firstClub, String secondClub){
        if (firstClub.contains(secondClub) || secondClub.contains(firstClub)) return true;

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

    public Long getId() {
        return id;
    }

    public Integer getExternalServicePlayerId() {
        return externalServicePlayerId;
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

    public String getExternalServicePlayerClub() {
        return externalServicePlayerClub;
    }

    public Boolean getCurrentlyInjured() {
        return currentlyInjured;
    }

    public PlayerLeagueStats getLeagueStats() {
        return leagueStats;
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

    public void setRecentMatches(List<PlayerRecentMatch> recentMatches) {
        this.recentMatches = recentMatches;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExternalServicePlayerId(Integer externalServicePlayerId) {
        this.externalServicePlayerId = externalServicePlayerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setRapidApiClub(String rapidApiClub) {
        this.rapidApiClub = rapidApiClub;
    }

    public void setExternalServicePlayerClub(String externalServicePlayerClub) {
        this.externalServicePlayerClub = externalServicePlayerClub;
    }

    public void setCurrentlyInjured(Boolean currentlyInjured) {
        this.currentlyInjured = currentlyInjured;
    }

    public void setLeagueStats(PlayerLeagueStats leagueStats) {
        this.leagueStats = leagueStats;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setClubPhotoUrl(String clubPhotoUrl) {
        this.clubPhotoUrl = clubPhotoUrl;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
