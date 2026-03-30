package com.example.bombavafrontmovil;

import java.util.ArrayList;
import java.util.List;

public class BarcoLogico {
    public String id;
    public int tipo;          // tamaño en casillas
    public int x, y;          // ANCLA del barco (popa / celda inicial)
    public String orientation; // "N", "S", "E", "W"
    public boolean esAliado;
    public int[] vidaCeldas;
    public int[] danoArmas;
    public String slug;

    public int hpActual = 1;
    public int hpMax = 1;

    public BarcoLogico() {}

    public BarcoLogico(String id, int tipo, int x, int y, String orientation, boolean esAliado, String slug) {
        this.id = id;
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.orientation = orientation;
        this.esAliado = esAliado;
        this.slug = slug;

        int tam = Math.max(tipo, 1);
        this.vidaCeldas = new int[tam];
        this.danoArmas = new int[tam];

        for (int i = 0; i < tam; i++) {
            this.vidaCeldas[i] = 3;
            this.danoArmas[i] = 100;
        }
    }

    public boolean isEsAliado() {
        return esAliado;
    }

    public List<int[]> getCeldas() {
        return getCeldasPara(x, y, orientation, tipo);
    }

    /**
     * x,y se interpreta como el extremo de popa / celda inicial.
     * La proa es la última celda del recorrido.
     *
     * Devuelve: { fila(y), columna(x), esProa(1/0), indiceEnBarco }
     */
    public static List<int[]> getCeldasPara(int x, int y, String orientation, int tipo) {
        List<int[]> celdas = new ArrayList<>();

        int dx = 0;
        int dy = 0;

        if ("N".equals(orientation)) {
            dy = -1;
        } else if ("S".equals(orientation)) {
            dy = 1;
        } else if ("E".equals(orientation)) {
            dx = 1;
        } else if ("W".equals(orientation)) {
            dx = -1;
        }

        int tam = Math.max(tipo, 1);

        for (int i = 0; i < tam; i++) {
            int cx = x + dx * i;
            int cy = y + dy * i;
            boolean esProa = (i == tam - 1) && tam > 1;

            celdas.add(new int[]{cy, cx, esProa ? 1 : 0, i});
        }

        return celdas;
    }
}