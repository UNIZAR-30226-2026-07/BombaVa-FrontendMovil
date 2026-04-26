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

    public String tipo;

    // Añade 'tipo' al final del constructor
    public TorpedoLogico(String id, int x, int y, int vectorX, int vectorY, int lifeDistance, boolean esAliado, String tipo) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vectorX = vectorX;
        this.vectorY = vectorY;
        this.lifeDistance = lifeDistance;
        this.esAliado = esAliado;
        this.tipo = tipo;

        // Traducimos el vector (Esto afecta a los torpedos, las minas no se mueven)
        if (vectorY == -1) this.direccion = "N";
        else if (vectorY == 1) this.direccion = "S";
        else if (vectorX == 1) this.direccion = "E";
        else if (vectorX == -1) this.direccion = "W";
        else this.direccion = "N";
    }
}