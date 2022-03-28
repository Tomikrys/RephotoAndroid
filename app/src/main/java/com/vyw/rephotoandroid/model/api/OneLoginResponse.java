package com.vyw.rephotoandroid.model.api;

import com.google.gson.annotations.SerializedName;

public class OneLoginResponse {

    @SerializedName("data")
    private LoginResponse loginResponse;

    public OneLoginResponse(LoginResponse loginResponse) {
        this.loginResponse = loginResponse;
    }

    public LoginResponse getLoginResponse() {
        return loginResponse;
    }

    public void setLoginResponse(LoginResponse loginResponse) {
        this.loginResponse = loginResponse;
    }
}
