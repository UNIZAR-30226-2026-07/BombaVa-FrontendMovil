package com.example.bombavafrontmovil;

import android.util.Log;
import com.example.bombavafrontmovil.models.Position;
import com.example.bombavafrontmovil.models.ShipPosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GestorConfiguracionFlota {
    private int[] tablero = new int[225];
    private Map<Integer, Set<String>> armasPorBarco = new HashMap<>();
    private int idBarcoActual = 1;

    public int getIdBarcoActual() { return idBarcoActual; }

    public int ajustarPosicion(int pos, int tamano, boolean enHorizontal) {
        int fila = pos / 15;
        int col = pos % 15;
        if (enHorizontal && (col + tamano > 15)) col = 15 - tamano;
        if (!enHorizontal && (fila + tamano > 15)) fila = 15 - tamano;
        return (fila * 15) + col;
    }

    public int validarColocacion(int posAjustada, int tamano, boolean enHorizontal) {
        int fila = posAjustada / 15;
        if (fila < 10) return 1; // Solo zona aliada
        for (int i = 0; i < tamano; i++) {
            int check = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            if (check >= 225) return 2; // Protección anti-desbordamiento
            if (tablero[check] != 0) return 2;
        }
        return 0;
    }

    public void colocarBarco(int posAjustada, int tamano, boolean enHorizontal) {
        for (int i = 0; i < tamano; i++) {
            int p = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            if(p < 225) tablero[p] = idBarcoActual;
        }
        idBarcoActual++;
    }

    public void borrarBarco(int idBarco) {
        for (int i = 0; i < 225; i++) {
            if (tablero[i] == idBarco) tablero[i] = 0;
        }
        armasPorBarco.remove(idBarco);
    }

    public int getIdBarcoEn(int pos) { return pos >= 0 && pos < 225 ? tablero[pos] : 0; }

    public int getTamanoBarco(int idBarco) {
        int count = 0;
        for (int id : tablero) { if (id == idBarco) count++; }
        return count;
    }

    public boolean estaBarcoColocado(int tamanoBuscado) {
        for (int i = 1; i < idBarcoActual; i++) {
            if (getTamanoBarco(i) == tamanoBuscado) return true;
        }
        return false;
    }

    public void equiparArma(int idBarco, String arma) {
        armasPorBarco.putIfAbsent(idBarco, new HashSet<>());
        armasPorBarco.get(idBarco).add(arma);
    }

    public void desequiparArma(int idBarco, String arma) {
        if (armasPorBarco.containsKey(idBarco)) armasPorBarco.get(idBarco).remove(arma);
    }

    public Set<String> getArmasEquipadas(int idBarco) {
        return armasPorBarco.getOrDefault(idBarco, new HashSet<>());
    }

    public boolean tieneArmaEquipada(int idBarco, String arma) {
        return armasPorBarco.containsKey(idBarco) && armasPorBarco.get(idBarco).contains(arma);
    }

    public List<ShipPosition> obtenerPosicionesParaBackend(String id5, String id3, String id1) {
        List<ShipPosition> posiciones = new ArrayList<>();
        for (int idInterno = 1; idInterno < idBarcoActual; idInterno++) {
            int inicio = -1;
            for (int i = 0; i < 225; i++) {
                if (tablero[i] == idInterno) { inicio = i; break; }
            }
            if (inicio != -1) {
                int tamano = getTamanoBarco(idInterno);

                // Mapeo robusto: Si falla el tamaño exacto, asume por descarte
                String realId = (tamano >= 4) ? id5 : (tamano >= 2 ? id3 : id1);

                if (realId == null || realId.isEmpty()) {
                    Log.e("FLOTA_DEBUG", "ID de barco nulo detectado al guardar. Ignorando barco.");
                    continue;
                }

                // Si es tamaño 1, la orientación no importa, asignamos "N" por defecto
                String orientacion = "N";
                if (tamano > 1) {
                    boolean esH = (inicio + 1 < 225 && tablero[inicio + 1] == idInterno);
                    orientacion = esH ? "E" : "N";
                }

                int x = inicio % 15;
                int yCorregida = (inicio / 15) - 10; // ESENCIAL: De 10-14 a 0-4

                posiciones.add(new ShipPosition(realId, new Position(x, yCorregida), orientacion));
            }
        }
        return posiciones;
    }

    public void cargarBarcoDesdeServidor(String realId, int x, int yServidor, String orientacion, String id5, String id3, String id1) {
        int yApp = yServidor + 10;
        int posInicio = (yApp * 15) + x;
        int tamano = (realId.equals(id5)) ? 5 : (realId.equals(id3) ? 3 : (realId.equals(id1) ? 1 : 0));

        if (tamano > 0) {
            boolean horizontal = orientacion.toUpperCase().equals("E") || orientacion.toUpperCase().equals("H");
            for (int i = 0; i < tamano; i++) {
                int p = horizontal ? (posInicio + i) : (posInicio + (i * 15));
                if (p < 225) tablero[p] = idBarcoActual;
            }
            idBarcoActual++;
        }
    }

    public void resetearTablero() {
        tablero = new int[225];
        armasPorBarco.clear();
        idBarcoActual = 1;
    }
}