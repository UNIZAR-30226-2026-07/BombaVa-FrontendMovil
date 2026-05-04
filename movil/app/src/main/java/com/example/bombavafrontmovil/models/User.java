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

    // --- NUEVOS CAMPOS ---
    @SerializedName("wins")
    private int wins;

    @SerializedName("totalGames")
    private int totalGames;

    @SerializedName("winRate")
    private double winRate;

    public String getUsername() { return username; }
    public int getEloRating() { return eloRating; }
    public String getEmail() { return email; }
    public String getId() { return id; }

    // --- NUEVOS GETTERS ---
    public int getWins() { return wins; }
    public int getTotalGames() { return totalGames; }
    public double getWinRate() { return winRate; }
}