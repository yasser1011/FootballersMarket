package com.yasser.footballersmarket.footballer;

import javax.persistence.*;
import java.util.List;

@Entity
public class Footballer {
    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long sofascoreId;
    private String name;
    private String firstName;
    private String lastName;
    private String nationality;
    private Integer age;
    private String position;
    private String club;
    private Boolean currentlyInjured;
    private Integer goals;
    private Integer assists;
    private Double generalRating;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_id")
    private List<Rating> recentRatings;
    @Transient
    private Boolean rising;
    private String strengths;
    private String weaknesses;

    public Footballer(){}

    public Footballer(Long id, String name, String nationality, Integer age, String position, String club, Boolean currentlyInjured, Integer goals, Integer assists, List<Rating> recentRatings) {
        this.id = id;
        this.name = name;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.club = club;
        this.currentlyInjured = currentlyInjured;
        this.goals = goals;
        this.assists = assists;
        this.recentRatings = recentRatings;
    }

    public Footballer(Long id, Long sofascoreId, String name, String firstName, String lastName, String nationality, Integer age, String position, String club, Boolean currentlyInjured, Integer goals, Integer assists, Double generalRating, List<Rating> recentRatings, String strengths, String weaknesses) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.club = club;
        this.currentlyInjured = currentlyInjured;
        this.goals = goals;
        this.assists = assists;
        this.generalRating = generalRating;
        this.recentRatings = recentRatings;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
    }

    public Footballer(Long id, String name, String nationality, Integer age, String position, String club, Boolean currentlyInjured, Integer goals, Integer assists, List<Rating> recentRatings, String strengths, String weaknesses) {
        this.id = id;
        this.name = name;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.club = club;
        this.currentlyInjured = currentlyInjured;
        this.goals = goals;
        this.assists = assists;
        this.recentRatings = recentRatings;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
    }

    public Footballer(String name, String nationality, Integer age, String position, String club, Boolean currentlyInjured, Integer goals, Integer assists, List<Rating> recentRatings) {
        this.name = name;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.club = club;
        this.currentlyInjured = currentlyInjured;
        this.goals = goals;
        this.assists = assists;
        this.recentRatings = recentRatings;
    }

    public Footballer(Long id, Long sofascoreId, String name, String firstName, String lastName, String nationality, Integer age, String position, String club, Boolean currentlyInjured, Integer goals, Integer assists, Double generalRating, List<Rating> recentRatings, Boolean rising, String strengths, String weaknesses) {
        this.id = id;
        this.sofascoreId = sofascoreId;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationality = nationality;
        this.age = age;
        this.position = position;
        this.club = club;
        this.currentlyInjured = currentlyInjured;
        this.goals = goals;
        this.assists = assists;
        this.generalRating = generalRating;
        this.recentRatings = recentRatings;
        this.rising = rising;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
    }

    public void addRating(Rating newRating){
        if(recentRatings.size() == 5){
            recentRatings.remove(0);
        }
        recentRatings.add(newRating);
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

    public Boolean getRising() {
        return rising;
    }

    public void setRising(Boolean rising) {
        this.rising = rising;
    }

    public String getStrengths() {
        return strengths;
    }

    public Long getSofascoreId() {
        return sofascoreId;
    }

    public void setSofascoreId(Long sofascoreId) {
        this.sofascoreId = sofascoreId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Double getGeneralRating() {
        return generalRating;
    }

    public void setGeneralRating(Double generalRating) {
        this.generalRating = generalRating;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public Boolean getCurrentlyInjured() {
        return currentlyInjured;
    }

    public void setCurrentlyInjured(Boolean currentlyInjured) {
        this.currentlyInjured = currentlyInjured;
    }

    public Integer getGoals() {
        return goals;
    }

    public void setGoals(Integer goals) {
        this.goals = goals;
    }

    public Integer getAssists() {
        return assists;
    }

    public void setAssists(Integer assists) {
        this.assists = assists;
    }

    public List<Rating> getRecentRatings() {
        return recentRatings;
    }

    public void setRecentRatings(List<Rating> recentRatings) {
        this.recentRatings = recentRatings;
    }
}
