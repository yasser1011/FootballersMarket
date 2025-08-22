package com.yasser.footballersmarket.user;

public class UserResponse {
    private Long userId;
    private String username;
    private Integer points;

    public UserResponse(){}
    public UserResponse(Long userId, String username, Integer points) {
        this.userId = userId;
        this.username = username;
        this.points = points;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Integer getPoints() {
        return points;
    }
}
