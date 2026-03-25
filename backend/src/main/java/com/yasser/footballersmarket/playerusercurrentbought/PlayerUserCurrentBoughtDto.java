package com.yasser.footballersmarket.playerusercurrentbought;

import java.time.LocalDate;

public class PlayerUserCurrentBoughtDto {
    private Long id;
    private Integer externalServicePlayerId;
    private String name;
    private String photoUrl;
    private String externalServicePlayerClub;
    private String clubPhotoUrl;
    private String nationality;
    private LocalDate dateOfBirth;
    private String position;
    private String updatedBy;
    private Integer totalGoals;
    private Integer totalAssists;
    private Double avgRating;
    private Integer buyPrice;
    private Integer price;
    private Boolean areClubStatsUpdated;

    public PlayerUserCurrentBoughtDto(){}

    public PlayerUserCurrentBoughtDto(Long playerId, Integer externalServicePlayerId, String playerName, String playerPhotoUrl, String externalServicePlayerClub, String clubPhotoUrl, String nationality, LocalDate dateOfBirth, String position, String updatedBy, Integer goals, Integer assists, Double rating, Integer buyPrice, Integer price, Boolean areClubStatsUpdated) {
        this.id = playerId;
        this.externalServicePlayerId = externalServicePlayerId;
        this.externalServicePlayerClub = externalServicePlayerClub;
        this.clubPhotoUrl = clubPhotoUrl;
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
        this.position = position;
        this.updatedBy = updatedBy;
        this.totalGoals = goals;
        this.totalAssists = assists;
        this.avgRating = rating;
        this.buyPrice = buyPrice;
        this.price = price;
        this.name = playerName;
        this.photoUrl = playerPhotoUrl;
        this.areClubStatsUpdated = areClubStatsUpdated;
    }

    public Long getId() {
        return id;
    }

    public Integer getExternalServicePlayerId() {
        return externalServicePlayerId;
    }

    public Boolean getAreClubStatsUpdated() {
        return areClubStatsUpdated;
    }

    public String getName() {
        return name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getExternalServicePlayerClub() {
        return externalServicePlayerClub;
    }

    public String getClubPhotoUrl() {
        return clubPhotoUrl;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Integer getTotalGoals() {
        return totalGoals;
    }

    public Integer getTotalAssists() {
        return totalAssists;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public Integer getBuyPrice() {
        return buyPrice;
    }

    public Integer getPrice() {
        return price;
    }
}
