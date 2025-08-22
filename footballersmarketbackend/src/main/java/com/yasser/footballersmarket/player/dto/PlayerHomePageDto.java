package com.yasser.footballersmarket.player.dto;

public class PlayerHomePageDto {
    private Long id;
    private String name;
    private String imgUrl;
    private String club;
    private String clubImgUrl;
    private String nationality;
    private Integer age;
    private String position;
    private Double rating;
    private Integer goals;
    private Integer assists;
    private Double price;

    public PlayerHomePageDto(Long id, String name, String imgUrl,String club,
                             String clubImgUrl, String nationality, Integer age, String position, Double rating, Integer goals,Integer assists, Double price) {
        this.id = id;
        this.name = name;
        this.goals = goals;
        this.imgUrl = imgUrl;
        this.club = club;
        this.clubImgUrl = clubImgUrl;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.rating = rating;
        this.assists = assists;
        this.price = price;
    }

    public PlayerHomePageDto(Long id, String name, String imgUrl,String club,
                             String clubImgUrl, String nationality, Integer age, String position, Double rating, Integer goals,Integer assists) {
        this.id = id;
        this.name = name;
        this.goals = goals;
        this.imgUrl = imgUrl;
        this.club = club;
        this.clubImgUrl = clubImgUrl;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.rating = rating;
        this.assists = assists;

    }


    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getClub() {
        return club;
    }

    public String getClubImgUrl() {
        return clubImgUrl;
    }

    public String getNationality() {
        return nationality;
    }

    public Integer getAge() {
        return age;
    }

    public String getPosition() {
        return position;
    }

    public Double getRating() {
        return rating;
    }

    public Integer getGoals() {
        return goals;
    }

    public Integer getAssists() {
        return assists;
    }

    public Double getPrice() {
        return price;
    }
}
