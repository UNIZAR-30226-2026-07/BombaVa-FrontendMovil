package com.example.bombavafrontmovil;

public class TorpedoLogico {
    public String id;
    public int x;
    public int y;
    public int vectorX;
    public int vectorY;
    public int lifeDistance;

    public boolean esAliado;
    public String direccion;

    public TorpedoLogico(String id, int x, int y, int vectorX, int vectorY, int lifeDistance, boolean esAliado) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vectorX = vectorX;
        this.vectorY = vectorY;
        this.lifeDistance = lifeDistance;
        this.esAliado = esAliado;

        // Traducimos el vector del servidor a la dirección visual de tu tablero
        if (vectorY == -1) this.direccion = "N";
        else if (vectorY == 1) this.direccion = "S";
        else if (vectorX == 1) this.direccion = "E";
        else if (vectorX == -1) this.direccion = "W";
        else this.direccion = "N"; // Por defecto
    }

    // Método de ayuda para que el Board sepa hacia dónde mira el torpedo
    public String getOrientation() {
        if (vectorY == -1) return "N";
        if (vectorY == 1) return "S";
        if (vectorX == 1) return "E";
        if (vectorX == -1) return "W";
        return "N";
    }
}
