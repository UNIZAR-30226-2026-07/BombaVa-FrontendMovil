package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class User {
    private String id;

    @SerializedName("elo_rating")
    private int eloRating;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    public String getUsername() { return username; }
    public int getEloRating() {
        return eloRating;
    }
    public String getEmail() { return email; }
    public String getId() { return id; }
}