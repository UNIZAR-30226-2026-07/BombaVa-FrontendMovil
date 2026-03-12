package com.example.bombavafrontmovil.models;
import com.google.gson.annotations.SerializedName;

public class ShipTemplate {
    @SerializedName("slug")
    private String slug;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;

    // Un método útil para saber el tamaño real en nuestro tablero (ej: si es 5x1 o 1x5, el tamaño es 5)
    public int getTamanoCasillas() {
        return Math.max(width, height);
    }
}