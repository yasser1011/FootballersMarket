package com.yasser.footballersmarket.footballer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double rating;
    private Date updatedOn;
    @ManyToOne
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    @JsonIgnoreProperties("recentRatings")
    private Footballer player;

    public Rating(){}

    public Rating(Double rating, Date updatedOn) {
        this.rating = rating;
        this.updatedOn = updatedOn;
    }

    public Rating(Double rating, Date updatedOn, Footballer player) {
        this.rating = rating;
        this.updatedOn = updatedOn;
        this.player = player;
    }

    public Rating(Long id, Double rating, Date updatedOn, Footballer player) {
        this.id = id;
        this.rating = rating;
        this.updatedOn = updatedOn;
        this.player = player;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Footballer getPlayer() {
        return player;
    }

    public void setPlayer(Footballer player) {
        this.player = player;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }
}
