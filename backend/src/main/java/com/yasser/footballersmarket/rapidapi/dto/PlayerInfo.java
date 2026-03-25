package com.yasser.footballersmarket.rapidapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

public class PlayerInfo {
    private Long id;
    private String name;
    private String firstname;
    private String lastname;
    private LocalDate dateOfBirth;
    private String nationality;
    private String photo;
    private Boolean injured;
    private Integer age;

    public PlayerInfo(){}

    public PlayerInfo(Long id, String name, LocalDate dateOfBirth, String nationality, String photoUrl, Boolean injured, Integer age, String firstname, String lastname) {
        this.id = id;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.photo = photoUrl;
        this.injured = injured;
        this.age = age;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    @JsonProperty("birth")
    private void getPlayerDateOfBirth(Map<String,Object> entity) throws ParseException {
        String dobRes = (String)entity.get("date");
        if (dobRes == null){
            Integer age = this.age;
            if(age != null){
                this.dateOfBirth = calculateDob(age);
            }else{
                int DEFAULT_AGE = 16;
                this.dateOfBirth = calculateDob(DEFAULT_AGE);
            }
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.dateOfBirth = LocalDate.parse(dobRes, formatter);
    }


    public Long getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }


    public String getNationality() {
        return nationality;
    }


    public String getPhoto() {
        return photo;
    }


    public Boolean getInjured() {
        return injured;
    }

    public Integer getAge() {
        return age;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    private LocalDate calculateDob(int ageInYears) {
        LocalDate currentDate = LocalDate.now();
        LocalDate dob = currentDate.minusYears(ageInYears);

        if (dob.isAfter(currentDate)) {
            dob = dob.minusYears(1);
        }
        return dob;
    }
}
