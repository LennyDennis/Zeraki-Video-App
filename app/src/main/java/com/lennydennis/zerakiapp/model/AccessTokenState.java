package com.lennydennis.zerakiapp.model;

public class AccessTokenState {
    private String accessToken;
    private Throwable throwable;

    public AccessTokenState(String accessToken) {
        this.accessToken = accessToken;
        this.throwable = null;
    }

    public AccessTokenState(Throwable throwable) {
        this.accessToken = null;
        this.throwable = throwable;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
