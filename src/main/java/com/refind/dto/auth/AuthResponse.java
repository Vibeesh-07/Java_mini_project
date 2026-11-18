package com.refind.dto.auth;

public class AuthResponse {

    private String message;
    private boolean authenticated;
    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String message, boolean authenticated, UserResponse user) {
        this.message = message;
        this.authenticated = authenticated;
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
