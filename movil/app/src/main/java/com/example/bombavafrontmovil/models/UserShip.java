package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserShip {
    @SerializedName("id")
    private String id;

    @SerializedName("ShipTemplate")
    private ShipTemplate shipTemplate;

    @SerializedName("equippedWeaponSlug")
    private String weaponSlug;

    @SerializedName("equippedWeapons")
    private List<String> weaponSlugs;

    @SerializedName("weapons")
    private List<WeaponItem> weaponsList;

    @SerializedName("ShipWeapons")
    private List<WeaponItem> shipWeapons;

    @SerializedName("equipped")
    private List<WeaponItem> equipped;

    @SerializedName("WeaponTemplates")
    private List<WeaponItem> weaponTemplates;

    public static class WeaponItem {
        // Volvemos a poner "slug", que es como viene en tu JSON
        @SerializedName("slug")
        public String slug;
    }

    // Getters
    public String getId() { return id; }
    public ShipTemplate getShipTemplate() { return shipTemplate; }
    public String getWeaponSlug() { return weaponSlug; }
    public List<String> getWeaponSlugs() { return weaponSlugs; }
    public List<WeaponItem> getShipWeapons() { return shipWeapons; }
    public List<WeaponItem> getEquipped() { return equipped; }
    public List<WeaponItem> getWeaponsList() { return weaponsList; }

    public List<WeaponItem> getWeaponTemplates() { return weaponTemplates; }
}