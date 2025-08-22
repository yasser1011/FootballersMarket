package com.yasser.footballersmarket.user.dto;

import com.yasser.footballersmarket.user.UserResponse;

public class AuthResponse {
    private Integer status;
    private String errMsg;
    private String token;
    private UserResponse user;

    public AuthResponse(){}

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public AuthResponse(Integer status, String errMsg, String token, UserResponse user) {
        this.status = status;
        this.errMsg = errMsg;
        this.token = token;
        this.user = user;
    }

    public AuthResponse(Integer status, String errMsg) {
        this.status = status;
        this.errMsg = errMsg;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public Integer getStatus() {
        return status;
    }

    public String getErrMsg() {
        return errMsg;
    }
}
