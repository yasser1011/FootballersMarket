package com.yasser.footballersmarket.player;

import com.yasser.footballersmarket.playerstats.PlayerChampionsLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerLeagueStats;
import com.yasser.footballersmarket.playerstats.PlayerWorldCupStats;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

@Entity
public class Player {
    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
    // rapid api scheduler id
    private Long id;
    private Integer sofascoreId;
    private String name;
    private String nationality;
    private LocalDate dateOfBirth;
    private String position;
    // will use this property to check if stats are correct by comparing his team from rapid api scheduler
    // and his actual current club from sofascore if they're the same then scheduler stats are updated
    // if not then his current team stats are not in scope of scheduler stats fetching
    private String rapidApiClub;
    private String sofascoreClub;
    private Boolean currentlyInjured;
//    @Transient
//    private Integer totalGoals;
//    @Transient
//    private Integer totalAssists;
//    @Transient
//    private Double avgRating;

//    @Transient
//    private Boolean areClubStatsUpdated;

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private PlayerLeagueStats leagueStats;

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private PlayerChampionsLeagueStats championsLeagueStats;

    // tournament stats; null for players not in the world cup. read by the pricing strategy
    @OneToOne(mappedBy = "player")
    @PrimaryKeyJoinColumn
    private PlayerWorldCupStats worldCupStats;
//
//    @Transient
//    private Integer price;

    private String photoUrl;
    private String clubPhotoUrl;

    private String updatedBy;

    public Player(){}
//
//    public Player(Long id, String name) {
//        this.id = id;
//        this.name = name;
//    }
//
//    public Player(Long id, String name, PlayerLeagueStats leagueStats, PlayerChampionsLeagueStats championsLeagueStats) {
//        this.id = id;
//        this.name = name;
//        this.leagueStats = leagueStats;
//        this.championsLeagueStats = championsLeagueStats;
//    }
//
//
    public Player(Long id, Integer sofascoreId, String name, String club, String updatedBy) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.name = name;
        this.rapidApiClub = club;
        this.updatedBy = updatedBy;
    }
    public Player(Long id, Integer sofascoreId, String rapidApiClub, String sofascoreClub, String photoUrl, String clubPhotoUrl) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.rapidApiClub = rapidApiClub;
        this.sofascoreClub = sofascoreClub;
        this.photoUrl = photoUrl;
        this.clubPhotoUrl = clubPhotoUrl;
    }

    public Player(Long id, Integer sofascoreId, String name, LocalDate dateOfBirth, String club, String sofascoreClub, String updatedBy) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.name = name;
        this.rapidApiClub = club;
        this.sofascoreClub = sofascoreClub;
        this.updatedBy = updatedBy;
        this.dateOfBirth = dateOfBirth;
    }
    public Player(Long id, Integer sofascoreId, String name, LocalDate dateOfBirth, String club, String sofascoreClub,
                  String updatedBy, PlayerLeagueStats leagueStats) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.name = name;
        this.rapidApiClub = club;
        this.sofascoreClub = sofascoreClub;
        this.updatedBy = updatedBy;
        this.dateOfBirth = dateOfBirth;
        this.leagueStats = leagueStats;
    }

    public Player(Long id, String rapidApiClub, String sofascoreClub) {
        this.id = id;
        this.rapidApiClub = rapidApiClub;
        this.sofascoreClub = sofascoreClub;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getSofascoreId() {
        return sofascoreId;
    }

    public void setSofascoreId(Integer sofascoreId) {
        this.sofascoreId = sofascoreId;
    }


//    public Double getAvgRating() {
//        Double avgRating = 0.0;
//        Double clRateW = 0.7;
//        Double leagueRateW = 0.3;
//
//        if(championsLeagueStats == null && leagueStats == null)
//            return 6.5;
//
//        if(championsLeagueStats == null){
//            double result = Math.round(leagueStats.getRating() * 100.0) / 100.0;
//            return result;
//        }
//        if (leagueStats == null){
//            double result = Math.round(championsLeagueStats.getRating() * 100.0) / 100.0;
//            return result;
//        }
//
//        double result = leagueStats.getRating() * leagueRateW + championsLeagueStats.getRating() * clRateW;
//        result = Math.round(result * 100.0) / 100.0;
//        return result;
//    }

//    public void setAvgRating(Double avgRating) {
//        this.avgRating = avgRating;
//    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public PlayerLeagueStats getLeagueStats() {
        return leagueStats;
    }

    public void setLeagueStats(PlayerLeagueStats leagueStats) {
        this.leagueStats = leagueStats;
        this.leagueStats.setPlayer(this);
    }

    public PlayerWorldCupStats getWorldCupStats() {
        return worldCupStats;
    }

    public PlayerChampionsLeagueStats getChampionsLeagueStats() {
        return championsLeagueStats;
    }

    public void setChampionsLeagueStats(PlayerChampionsLeagueStats championsLeagueStats) {
        this.championsLeagueStats = championsLeagueStats;
    }

    public String getRapidApiClub() {
        return rapidApiClub;
    }

    public void setRapidApiClub(String club) {
        this.rapidApiClub = club;
    }

    public Boolean getCurrentlyInjured() {
        return currentlyInjured;
    }

    public void setCurrentlyInjured(Boolean currentlyInjured) {
        this.currentlyInjured = currentlyInjured;
    }

//    public Integer getTotalGoals() {
//        int totalGoals = 0;
//        if(leagueStats != null)
//            totalGoals += leagueStats.getGoals();
//        if (championsLeagueStats != null)
//            totalGoals += championsLeagueStats.getGoals();
//
//        return totalGoals;
//    }

//    public void setTotalGoals(Integer totalGoals) {
//        this.totalGoals = totalGoals;
//    }

//    public Integer getTotalAssists() {
//        int totalAssists = 0;
//        if(leagueStats != null)
//            totalAssists += leagueStats.getAssists();
//        if (championsLeagueStats != null)
//            totalAssists += championsLeagueStats.getAssists();
//
//        return totalAssists;
//    }
//
//    public void setTotalAssists(Integer totalAssists) {
//        this.totalAssists = totalAssists;
//    }


//    public Integer getPrice() {
//
//        int age = getAge();
//        int offsetValue = 100;
//        double ageW = 0.25;
//        double ratingW = 0.45;
//        double goalsW = 0.2;
//        double assistsW = 0.1;
//        if(age < 23 || age > 33){
//            ageW = 0.4;
//            ratingW = 0.3;
//        }
//        Double avgRating = getAvgRating();
//        Integer totalGoals = getTotalGoals();
//        Integer totalAssists = getTotalAssists();
//        double ageD = age;
//        double v = (Math.pow(1 / ageD, 3) * ageW) * 100000 * offsetValue;
//        double ratingV = (Math.pow(avgRating, 1.1) * ratingW) * offsetValue;
//        double goalsV = (goalsW * Math.pow(totalGoals, 0.8)) * offsetValue;
//        double assistsV = (assistsW * Math.pow(totalAssists, 0.8) * offsetValue);
//
//        double v1 = v + ratingV + goalsV + assistsV;
//        int roundPrice = (int) Math.round(v1);
//        return roundPrice;
//    }

    public String getClubPhotoUrl() {
        return clubPhotoUrl;
    }

    public void setClubPhotoUrl(String clubPhotoUrl) {
        this.clubPhotoUrl = clubPhotoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getSofascoreClub() {
        return sofascoreClub;
    }

    public void setSofascoreClub(String sofascoreClub) {
        this.sofascoreClub = sofascoreClub;
    }

//    public Boolean getAreClubStatsUpdated() {
//        if (rapidApiClub == null || sofascoreClub == null)
//            return false;
//        return isClubNamesSimilar(rapidApiClub, sofascoreClub);
//    }
//
//    private boolean isClubNamesSimilar(String firstClub, String secondClub){
//        String[] firstClubStrArr = firstClub.split(" ");
//        String[] secondClubStrArr = secondClub.split(" ");
//
//
//        int lengthToLoop = Math.min(Math.min(2, firstClubStrArr.length), Math.min(2, secondClubStrArr.length));
//
//        for (int i = 0; i < lengthToLoop; i++){
//            String wrdToCompare1 = firstClubStrArr[i].toLowerCase();
//            String wrdToCompare2 = secondClubStrArr[i].toLowerCase();
//            if (wrdToCompare1.equals("utd"))
//                wrdToCompare1 = "united";
//            if(wrdToCompare2.equals("utd"))
//                wrdToCompare2 = "united";
//
//            // 3 because of "man" word as "manchester"
//            int subIdx1 = Math.min(3, wrdToCompare1.length());
//            int subIdx2 = Math.min(3, wrdToCompare2.length());
//
//            String subWrd1 = wrdToCompare1.substring(0, subIdx1);
//            String subWrd2 = wrdToCompare2.substring(0, subIdx2);
//
//            if (!subWrd1.equals(subWrd2)){
//                return false;
//            }
//        }
//        return true;
//    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

//    private int getAge(){
//        LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
//
//        return Period.between(dateOfBirth, currentDate).getYears();
//    }
}
