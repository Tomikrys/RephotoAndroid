package com.vyw.rephotoandroid.model.api;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private Integer id;
    private String access_token;
    private String first_name;
    private String last_name;
    private String email;

    public LoginResponse(Integer id, String access_token, String first_name, String last_name, String email) {
        this.id = id;
        this.access_token = access_token;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
