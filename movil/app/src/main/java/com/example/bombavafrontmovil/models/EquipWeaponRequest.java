package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class EquipWeaponRequest {
    @SerializedName("weaponSlug")
    private String weaponSlug;

    public EquipWeaponRequest(String weaponSlug) {
        this.weaponSlug = weaponSlug;
    }
}