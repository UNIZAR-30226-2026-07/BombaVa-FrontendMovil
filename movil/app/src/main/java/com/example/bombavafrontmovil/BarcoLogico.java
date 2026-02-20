package com.example.bombavafrontmovil;

import java.util.ArrayList;
import java.util.List;

public class BarcoLogico {
    public int id, tipo, fCentro, cCentro, dir, vidaGeneral;
    public boolean esAliado;
    public int[] vidaCeldas;
    public int[] danoArmas;

    public BarcoLogico(int id, int tipo, int fCentro, int cCentro, int dir, boolean esAliado) {
        this.id = id; this.tipo = tipo; this.fCentro = fCentro;
        this.cCentro = cCentro; this.dir = dir; this.esAliado = esAliado;

        this.vidaGeneral = tipo * 300;
        this.vidaCeldas = new int[tipo];
        this.danoArmas = new int[tipo];

        for(int i = 0; i < tipo; i++) {
            this.vidaCeldas[i] = 3; // 3 toques para romperse
            this.danoArmas[i] = 100; // Daño base
        }
    }

    public BarcoLogico clonar() {
        BarcoLogico clon = new BarcoLogico(id, tipo, fCentro, cCentro, dir, esAliado);
        // Copiamos el estado de vidas actual también
        clon.vidaGeneral = this.vidaGeneral;
        System.arraycopy(this.vidaCeldas, 0, clon.vidaCeldas, 0, this.vidaCeldas.length);
        return clon;
    }

    public List<int[]> getCeldas() {
        List<int[]> celdas = new ArrayList<>();
        int df = 0, dc = 0;
        if(dir == 0) df = -1; else if(dir == 1) dc = 1; else if(dir == 2) df = 1; else if(dir == 3) dc = -1;

        int offset = tipo / 2;
        for (int i = -offset; i <= offset; i++) {
            boolean esProa = (i == offset && tipo > 1);
            int indiceCelda = i + offset;
            celdas.add(new int[]{fCentro + (df * i), cCentro + (dc * i), esProa ? 1 : 0, indiceCelda});
        }
        return celdas;
    }
}