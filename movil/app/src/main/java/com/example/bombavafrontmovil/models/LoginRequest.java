package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    // Le decimos a Android: "Cuando envíes esto al servidor, llámalo 'email'"
    @SerializedName("email")
    private String email;

    // Le decimos a Android: "Cuando envíes esto al servidor, llámalo 'contrasena'"
    @SerializedName("contrasena")
    private String contrasena;

    public LoginRequest(String email, String contrasena) {
        this.email = email;
        this.contrasena = contrasena;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}