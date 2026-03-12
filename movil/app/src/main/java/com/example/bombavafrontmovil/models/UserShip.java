package com.example.bombavafrontmovil.models;
import com.google.gson.annotations.SerializedName;

public class UserShip {
    @SerializedName("id")
    private String id;
    @SerializedName("equippedWeaponSlug")
    private String weaponSlug;
    @SerializedName("ShipTemplate")
    private ShipTemplate shipTemplate;

    public String getId() {
        return id;
    }

    public ShipTemplate getShipTemplate() {
        return shipTemplate;
    }

    public String getWeaponSlug() {
        return weaponSlug;
    }
}