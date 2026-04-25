package com.example.bombavafrontmovil;

import java.util.ArrayList;
import java.util.List;

public class BarcoLogico {
    public String id;
    public int tipo; // tamaño en casillas
    public int x, y; // coordenada central lógica
    public String orientation; // "N", "S", "E", "W"
    public boolean esAliado;
    public int[] vidaCeldas;
    public int[] danoArmas;
    public String slug;

    public int hpActual = 1;
    public int hpMax = 1;

    // NUEVO: rango de visión real recibido del backend
    public int visionRange = -1;

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

    public int getRangoVision() {
        // Si backend ya lo ha mandado, usamos ese valor
        if (visionRange >= 0) return visionRange;

        // Fallback temporal por si algún evento aún no lo manda
        if (tipo == 1) return 4;
        if (tipo == 3) return 3;
        if (tipo == 5) return 2;
        return 2;
    }

    public List<int[]> getCeldas() {
        return getCeldasPara(x, y, orientation, tipo);
    }

    /**
     * Devuelve {filaLogica, columna, esProa(1/0), indiceEnBarco}
     * x,y = centro del barco.
     */
    public static List<int[]> getCeldasPara(int x, int y, String orientation, int tipo) {
        List<int[]> celdas = new ArrayList<>();

        int df = 0;
        int dc = 0;

        if ("N".equals(orientation)) df = -1;
        else if ("S".equals(orientation)) df = 1;
        else if ("E".equals(orientation)) dc = 1;
        else if ("W".equals(orientation)) dc = -1;

        int tam = Math.max(tipo, 1);
        int offset = tam / 2;

        for (int i = -offset; i <= offset; i++) {
            int fila = y + (df * i);
            int col = x + (dc * i);
            boolean esProa = (i == offset && tam > 1);
            int indice = i + offset;

            celdas.add(new int[]{fila, col, esProa ? 1 : 0, indice});
        }

        return celdas;
    }
}