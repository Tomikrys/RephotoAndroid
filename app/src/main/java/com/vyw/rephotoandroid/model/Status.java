package com.vyw.rephotoandroid.model;

public class Status {
    private Boolean status;
    private String error;

    public Status(Boolean status, String error) {
        this.status = status;
        this.error = error;
    }

    public Status(Boolean status) {
        this.status = status;
        this.error = null;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
