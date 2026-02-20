package com.example.bombavafrontmovil;

import java.util.ArrayList;
import java.util.List;

public class GestorJuego {
    private List<BarcoLogico> flota = new ArrayList<>();

    public GestorJuego() {
        inicializarFlota();
    }

    public List<BarcoLogico> getFlota() { return flota; }

    private void inicializarFlota() {
        // ENEMIGOS (Arriba)
        flota.add(new BarcoLogico(201, 1, 1, 2, 2, false));
        flota.add(new BarcoLogico(202, 3, 2, 7, 1, false));
        flota.add(new BarcoLogico(203, 5, 2, 12, 2, false));

        // ALIADOS (Abajo)
        flota.add(new BarcoLogico(101, 1, 13, 2, 0, true));
        flota.add(new BarcoLogico(102, 3, 12, 7, 1, true));
        flota.add(new BarcoLogico(103, 5, 11, 12, 0, true));
    }

    public BarcoLogico obtenerBarco(int id) {
        for (BarcoLogico b : flota) { if (b.id == id) return b; }
        return null;
    }

    // Devuelve TRUE si el movimiento fue válido y se aplicó
    public boolean intentarMoverBarco(BarcoLogico barcoReal, int accion, List<Casilla> matriz) {
        if (barcoReal == null) return false;

        BarcoLogico clon = barcoReal.clonar();
        int df = 0, dc = 0;
        if(clon.dir == 0) df = -1; else if(clon.dir == 1) dc = 1; else if(clon.dir == 2) df = 1; else if(clon.dir == 3) dc = -1;

        switch (accion) {
            case 1: clon.fCentro += df; clon.cCentro += dc; break; // Adelante
            case 2: clon.fCentro -= df; clon.cCentro -= dc; break; // Atrás
            case 3: clon.dir = (clon.dir + 3) % 4; break; // Rotar Izq
            case 4: clon.dir = (clon.dir + 1) % 4; break; // Rotar Der
        }

        // Validación de límites y choques
        for (int[] celda : clon.getCeldas()) {
            int f = celda[0], c = celda[1];
            if (f < 0 || f >= 15 || c < 0 || c >= 15) return false; // Se sale

            Casilla casillaDestino = matriz.get(f * 15 + c);
            if (casillaDestino.isTieneBarco() && casillaDestino.getIdBarco() != clon.id) {
                return false; // Choca
            }
        }

        // Aplicamos el movimiento
        barcoReal.fCentro = clon.fCentro;
        barcoReal.cCentro = clon.cCentro;
        barcoReal.dir = clon.dir;
        return true;
    }
}