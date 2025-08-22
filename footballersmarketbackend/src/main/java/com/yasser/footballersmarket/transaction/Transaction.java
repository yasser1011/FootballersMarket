package com.yasser.footballersmarket.transaction;

import com.yasser.footballersmarket.player.Player;
import com.yasser.footballersmarket.user.User;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer transactionType;
    private Integer price;
    private LocalDateTime timestamp;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "player_id")
    private Long playerId;

    @ManyToOne
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    public Transaction(){}

    public Transaction(Long id, Integer transactionType, Integer price, LocalDateTime timestamp, Long userId, Long playerId) {
        this.id = id;
        this.transactionType = transactionType;
        this.price = price;
        this.timestamp = timestamp;
        this.userId = userId;
        this.playerId = playerId;
    }

    public Transaction(Integer transactionType, Integer price, LocalDateTime timestamp, Long userId, Long playerId) {
        this.transactionType = transactionType;
        this.price = price;
        this.timestamp = timestamp;
        this.userId = userId;
        this.playerId = playerId;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
