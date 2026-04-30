package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class RankingUser {
    @SerializedName("username")
    private String username;

    @SerializedName("elo_rating")
    private int eloRating;

    public String getUsername() { return username; }
    public int getEloRating() { return eloRating; }
}