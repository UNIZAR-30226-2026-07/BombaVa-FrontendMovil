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
    private Map<Integer, Float> rotacionPorBarco = new HashMap<>(); // Guarda la rotación de cada barco
    private int idBarcoActual = 1;

    public int getIdBarcoActual() { return idBarcoActual; }
    public int[] getTablero() { return tablero; }

    // Metodo para resetear la lógica cuando no hay mazo activo
    public void limpiarTodo() {
        tablero = new int[225];
        armasPorBarco.clear();
        rotacionPorBarco.clear();
        idBarcoActual = 1;
    }

    // Alias para que ConfigurarFlotaActivity no dé error si usa este nombre
    public void resetearTablero() {
        limpiarTodo();
    }

    // Devuelve qué barco hay en una celda específica
    public int getIdBarcoEn(int pos) {
        if (pos < 0 || pos >= 225) return 0;
        return tablero[pos];
    }

    public int getTamanoBarco(int id) {
        int count = 0;
        for (int val : tablero) if (val == id) count++;
        return count;
    }

    // Comprueba si un tipo de barco (por tamaño) ya está en el tablero
    public boolean estaBarcoColocado(int tamano) {
        for (int i = 1; i < idBarcoActual; i++) {
            if (getTamanoBarco(i) == tamano) return true;
        }
        return false;
    }

    public float getRotacionBarco(int id) {
        return rotacionPorBarco.getOrDefault(id, 0f);
    }

    public int ajustarPosicion(int pos, int tamano, boolean enHorizontal) {
        int fila = pos / 15;
        int col = pos % 15;
        if (enHorizontal && (col + tamano > 15)) col = 15 - tamano;
        if (!enHorizontal && (fila + tamano > 15)) fila = 15 - tamano;
        return (fila * 15) + col;
    }

    public int validarColocacion(int posAjustada, int tamano, boolean enHorizontal) {
        int fila = posAjustada / 15;
        if (fila < 10) return 1; // Solo se permite colocar en zona aliada (filas 10 a 14)

        for (int i = 0; i < tamano; i++) {
            int checkPos = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            if (checkPos >= 225 || tablero[checkPos] != 0) return 2; // Choque con otro barco o fuera de límites
        }
        return 0; // Colocación válida
    }

    public void colocarBarco(int posAjustada, int tamano, boolean enHorizontal) {
        for (int i = 0; i < tamano; i++) {
            int paintPos = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            tablero[paintPos] = idBarcoActual;
        }
        // Guardamos la rotación para la UI (0 si es horizontal, 90 si es vertical)
        rotacionPorBarco.put(idBarcoActual, enHorizontal ? 0f : 90f);
        idBarcoActual++;
    }

    public void borrarBarco(int idBarco) {
        for (int i = 0; i < tablero.length; i++) {
            if (tablero[i] == idBarco) {
                tablero[i] = 0;
            }
        }
        armasPorBarco.remove(idBarco);
        rotacionPorBarco.remove(idBarco);
    }

    public void equiparArma(int idBarco, String armaSlug) {
        Set<String> armas = armasPorBarco.getOrDefault(idBarco, new HashSet<>());
        armas.add(armaSlug);
        armasPorBarco.put(idBarco, armas);
    }

    public void desequiparArma(int idBarco, String armaSlug) {
        if (armasPorBarco.containsKey(idBarco)) {
            armasPorBarco.get(idBarco).remove(armaSlug);
        }
    }

    public Set<String> getArmasEquipadas(int idBarco) {
        return armasPorBarco.getOrDefault(idBarco, new HashSet<>());
    }

    public boolean tieneArmaEquipada(int idBarco, String armaSlug) {
        return armasPorBarco.containsKey(idBarco) && armasPorBarco.get(idBarco).contains(armaSlug);
    }

    public List<ShipPosition> obtenerPosicionesParaBackend(String realIdBarco5, String realIdBarco3, String realIdBarco1) {
        List<ShipPosition> posiciones = new ArrayList<>();

        for (int idInterno = 1; idInterno < idBarcoActual; idInterno++) {
            List<Integer> celdasBarco = new ArrayList<>();
            for (int i = 0; i < 225; i++) {
                if (tablero[i] == idInterno) celdasBarco.add(i);
            }
            if (celdasBarco.isEmpty()) continue;

            int tamano = celdasBarco.size();
            String realId = (tamano >= 4) ? realIdBarco5 : (tamano >= 2 ? realIdBarco3 : realIdBarco1);            if (realId == null || realId.isEmpty()) continue;

            boolean horizontal = false;
            String orientacion = "N";
            if (tamano > 1) {
                int primera = celdasBarco.get(0);
                int segunda = celdasBarco.get(1);
                horizontal = ((segunda - primera) == 1);
                orientacion = horizontal ? "E" : "N";
            }
        }

        for (Map.Entry<Integer, List<Integer>> entry : celdasBarco.entrySet()) {
            List<Integer> casillas = entry.getValue();
            int tamano = casillas.size();
            String realId = (tamano == 5) ? realIdBarco5 : (tamano == 3 ? realIdBarco3 : realIdBarco1);

            boolean enHorizontal = casillas.size() > 1 && (casillas.get(1) - casillas.get(0) == 1);
            String orientacion = enHorizontal ? "E" : "S";

            int centroIndex = casillas.size() / 2;
            int posCentro = casillas.get(centroIndex);

            int xCentro = posCentro % 15;
            int yCentroApp = posCentro / 15;

            // La zona aliada visual está en filas 10..14.
            // Para que en partida se vea igual tras el espejado del tablero,
            // la Y relativa al backend debe ir invertida dentro de ese bloque.
            int yCentroBackend = 14 - yCentroApp;

            posiciones.add(new ShipPosition(
                    realId,
                    new Position(xCentro, yCentroBackend),
                    orientacion
            ));
        }

        return posiciones;
    }

    public void cargarBarcoDesdeServidor(String realId, int xCentro, int yServidor, String orientacion,
                                         String id5, String id3, String id1) {
        int tamano = (realId.equals(id5)) ? 5 : (realId.equals(id3) ? 3 : (realId.equals(id1) ? 1 : 0));
        if (tamano <= 0) return;

        // Inversa exacta de obtenerPosicionesParaBackend()
        int yCentroApp = 14 - yServidor;

        boolean horizontal = orientacion != null &&
                (orientacion.equalsIgnoreCase("E") || orientacion.equalsIgnoreCase("H"));

        int offset = tamano / 2;

        // Guardamos cómo debe dibujarse en la UI
        rotacionPorBarco.put(idBarcoActual, horizontal ? 0f : 90f);

        int offset = tamano / 2;
        for (int i = -offset; i <= offset; i++) {
            int fila = horizontal ? yCentroApp : (yCentroApp + i);
            int col = horizontal ? (xCentro + i) : xCentro;

            if (fila >= 0 && fila < 15 && col >= 0 && col < 15) {
                tablero[fila * 15 + col] = idBarcoActual;
            }
        }

        idBarcoActual++;
    }
}