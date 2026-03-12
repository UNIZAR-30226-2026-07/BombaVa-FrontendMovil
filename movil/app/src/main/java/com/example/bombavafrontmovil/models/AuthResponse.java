package com.example.bombavafrontmovil.models;

public class AuthResponse {
    private String token;
    private User user;
    private String message; // Por si hay error

    // Getters y Setters
    public String getToken() { return token; }
    public User getUser() { return user; }
    public String getMessage() { return message; }
}