package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class User {
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getId() { return id; }
}