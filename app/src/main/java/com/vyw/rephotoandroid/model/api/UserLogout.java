package com.vyw.rephotoandroid.model.api;

public class UserLogout {
    private String access_token;

    public UserLogout(String access_token) {
        this.access_token = access_token;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
}
