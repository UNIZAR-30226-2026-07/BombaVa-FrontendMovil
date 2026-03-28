package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    String username;
    String email;

    @SerializedName("contrasena")
    String password;

    public RegisterRequest(String u, String e, String p) {
        this.username = u;
        this.email = e;
        this.password = p;
    }
}