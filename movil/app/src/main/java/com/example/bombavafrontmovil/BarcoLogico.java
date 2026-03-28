package com.example.bombavafrontmovil;

import java.util.ArrayList;
import java.util.List;

public class BarcoLogico {
    public String id; // Ahora es String para el Servidor
    public int tipo;
    public int x, y; // Coordenadas del servidor
    public String orientation; // "N", "S", "E", "W"
    public boolean esAliado;
    public int[] vidaCeldas;
    public int[] danoArmas;

    public BarcoLogico() {}

    public BarcoLogico(String id, int tipo, int x, int y, String orientation, boolean esAliado) {
        this.id = id; this.tipo = tipo; this.x = x; this.y = y;
        this.orientation = orientation; this.esAliado = esAliado;
        this.vidaCeldas = new int[tipo];
        this.danoArmas = new int[tipo];
        for(int i = 0; i < tipo; i++) {
            this.vidaCeldas[i] = 3;
            this.danoArmas[i] = 100;
        }
    }

    public List<int[]> getCeldas() {
        List<int[]> celdas = new ArrayList<>();
        int df = 0, dc = 0;
        if("N".equals(orientation)) df = -1;
        else if("S".equals(orientation)) df = 1;
        else if("E".equals(orientation)) dc = 1;
        else if("W".equals(orientation)) dc = -1;

        int offset = tipo / 2;
        for (int i = -offset; i <= offset; i++) {
            boolean esProa = (i == offset && tipo > 1);
            celdas.add(new int[]{y + (df * i), x + (dc * i), esProa ? 1 : 0, i + offset});
        }
        return celdas;
    }
}