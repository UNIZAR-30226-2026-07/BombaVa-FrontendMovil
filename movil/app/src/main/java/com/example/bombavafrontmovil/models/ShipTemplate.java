package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;

public class ShipTemplate {

    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;

    @SerializedName("baseMaxHp")
    private int baseMaxHp;

    @SerializedName("supplyCost")
    private int supplyCost;

    // --- GETTERS ---
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getBaseMaxHp() { return baseMaxHp; }
    public int getSupplyCost() { return supplyCost; }
    public int getTamanoCasillas() {
        return Math.max(width, height);
    }
}