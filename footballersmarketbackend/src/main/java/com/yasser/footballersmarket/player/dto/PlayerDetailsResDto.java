package com.yasser.footballersmarket.player.dto;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.scores365.dto.PlayerRecentMatch;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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

    private String photoUrl;
    private String clubPhotoUrl;
    private String updatedBy;
    private List<PlayerRecentMatch> recentMatches;

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

    public Integer getPrice() {
        final int offsetValue = 100;
//        double ageWeight = 0.25;
//        double ratingWeight = 0.45;
        double ageWeight = 0.35;
        double ratingWeight = 0.65;
        double goalsWeight = 0.2;
        double assistsWeight = 0.1;

        int age = getAge();
        if (age < 23 || age > 33) {
            ageWeight = 0.55;
            ratingWeight = 0.45;
        }

        double avgRating = getAvgRating();
        int totalGoals = getTotalGoals();
        int totalAssists = getTotalAssists();

        double ageComponent = Math.pow(1.0 / age, 3) * ageWeight * 100_000 * offsetValue;
        double ratingComponent = Math.pow(avgRating, 1.1) * ratingWeight * offsetValue;
        double goalsComponent = Math.pow(totalGoals, 0.8) * goalsWeight * offsetValue;
        double assistsComponent = Math.pow(totalAssists, 0.8) * assistsWeight * offsetValue;
//        double totalValue = ageComponent + ratingComponent + goalsComponent + assistsComponent;
        double totalValue = ageComponent + ratingComponent;

        return (int) Math.round(totalValue);
    }

    public Boolean getAreClubStatsUpdated() {
        if (rapidApiClub == null || externalServicePlayerClub == null)
            return false;
        return areClubNamesSimilar(rapidApiClub, externalServicePlayerClub);
    }

    private boolean areClubNamesSimilar(String firstClub, String secondClub){
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

    private int getAge(){
        LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(dateOfBirth, currentDate).getYears();
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
