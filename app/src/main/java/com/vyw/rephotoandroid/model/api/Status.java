package com.vyw.rephotoandroid.model.api;

public class Status {
    private Integer status;
    private String error;

    private Integer id;
    private String access_token;
    private String first_name;
    private String last_name;
    private String email;

    public Status(Integer status, String error) {
        this.status = status;
        this.error = error;
    }

    public Status(Integer status) {
        this.status = status;
        this.error = null;
    }

    public Status(Integer status, Integer id, String access_token, String first_name, String last_name, String email) {
        this.status = status;
        this.id = id;
        this.access_token = access_token;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.error = null;
    }

    public Status(Integer id, String access_token, String first_name, String last_name, String email) {
        this.status = null;
        this.id = id;
        this.access_token = access_token;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.error = null;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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
