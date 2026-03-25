package com.yasser.footballersmarket.playerusercurrentbought;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.user.User;

import javax.persistence.*;

@Entity
public class PlayerUserCurrentBought {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer buyPrice;

    @Column(name = "user_id")
    private Long userId;

    // many records mapped to one user
    // in user table you can create one to many PlayerUserCurrentBought not one to one
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "player_id")
    private Long playerId;

    // many records mapped to one player
    @ManyToOne
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    PlayerUserCurrentBought(){}

    public PlayerUserCurrentBought(Long id, Long userId, Long playerId, Integer buyPrice) {
        this.id = id;
        this.userId = userId;
        this.playerId = playerId;
        this.buyPrice = buyPrice;
    }

    public PlayerUserCurrentBought(Long userId, Long playerId, Integer buyPrice) {
        this.userId = userId;
        this.playerId = playerId;
        this.buyPrice = buyPrice;
    }

    public PlayerUserCurrentBought(Long id, Long userId, User user, Long playerId, Player player, Integer buyPrice) {
        this.id = id;
        this.userId = userId;
        this.user = user;
        this.playerId = playerId;
        this.player = player;
        this.buyPrice = buyPrice;
    }

    public PlayerUserCurrentBought(Long id, Integer buyPrice, Long userId, Long playerId, Player player) {
        this.id = id;
        this.buyPrice = buyPrice;
        this.userId = userId;
        this.playerId = playerId;
        this.player = player;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Player getPlayer() {
        return player;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(Integer buyPrice) {
        this.buyPrice = buyPrice;
    }
}
