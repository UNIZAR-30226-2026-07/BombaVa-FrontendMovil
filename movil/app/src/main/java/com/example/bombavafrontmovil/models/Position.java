package com.example.bombavafrontmovil.models;
import com.google.gson.annotations.SerializedName;

public class Position {
    @SerializedName("x")
    private int x;
    @SerializedName("y")
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}