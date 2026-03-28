package com.example.bombavafrontmovil.models;

public class Weapon {
    private String slug;
    private String name;
    private String description;
    private String type;
    private int damage;
    private int apCost;
    private int range;
    private int lifeDistance;

    // Getters
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public int getDamage() { return damage; }
    public int getApCost() { return apCost; }
    public int getRange() { return range; }
    public int getLifeDistance() { return lifeDistance; }
}