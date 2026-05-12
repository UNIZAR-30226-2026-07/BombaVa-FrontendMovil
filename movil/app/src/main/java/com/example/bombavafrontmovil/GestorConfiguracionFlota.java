package com.example.bombavafrontmovil;

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
    private Map<Integer, Float> rotacionPorBarco = new HashMap<>();
    private Map<Integer, String> orientacionPorBarco = new HashMap<>();
    private int idBarcoActual = 1;

    public int getIdBarcoActual() {
        return idBarcoActual;
    }

    public int[] getTablero() {
        return tablero;
    }

    public void limpiarTodo() {
        tablero = new int[225];
        armasPorBarco.clear();
        rotacionPorBarco.clear();
        orientacionPorBarco.clear();
        idBarcoActual = 1;
    }

    public void resetearTablero() {
        limpiarTodo();
    }

    public int getIdBarcoEn(int pos) {
        if (pos < 0 || pos >= 225) return 0;
        return tablero[pos];
    }

    public int getTamanoBarco(int id) {
        int count = 0;
        for (int val : tablero) {
            if (val == id) count++;
        }
        return count;
    }

    public boolean estaBarcoColocado(int tamano) {
        for (int i = 1; i < idBarcoActual; i++) {
            if (getTamanoBarco(i) == tamano) return true;
        }
        return false;
    }

    public float getRotacionBarco(int id) {
        return rotacionPorBarco.getOrDefault(id, 0f);
    }

    public String getOrientacionBarco(int idBarco) {
        return orientacionPorBarco.getOrDefault(idBarco, "N");
    }

    private float convertirOrientacionAGrados(String orientacion) {
        switch (orientacion) {
            case "E":
                return 90f;
            case "S":
                return 180f;
            case "W":
                return 270f;
            case "N":
            default:
                return 0f;
        }
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
        if (fila < 10) return 1;

        for (int i = 0; i < tamano; i++) {
            int checkPos = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            if (checkPos >= 225 || tablero[checkPos] != 0) return 2;
        }

        return 0;
    }

    public void colocarBarco(int posAjustada, int tamano, boolean enHorizontal, String orientacion) {
        for (int i = 0; i < tamano; i++) {
            int paintPos = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            tablero[paintPos] = idBarcoActual;
        }

        orientacionPorBarco.put(idBarcoActual, orientacion);
        rotacionPorBarco.put(idBarcoActual, convertirOrientacionAGrados(orientacion));
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
        orientacionPorBarco.remove(idBarco);
    }

    public boolean equiparArma(int idBarco, String armaSlug) {
        Set<String> armas = armasPorBarco.getOrDefault(idBarco, new HashSet<>());

        if (armas.contains(armaSlug)) {
            return true;
        }

        int maxArmas = getMaxArmasPermitidas(idBarco);
        if (armas.size() >= maxArmas) {
            return false;
        }

        armas.add(armaSlug);
        armasPorBarco.put(idBarco, armas);
        return true;
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
        return armasPorBarco.containsKey(idBarco)
                && armasPorBarco.get(idBarco).contains(armaSlug);
    }

    private int getMaxArmasPermitidas(int idBarco) {
        int tamano = getTamanoBarco(idBarco);

        if (tamano == 1) return 1;
        if (tamano == 3) return 2;
        if (tamano == 5) return 3;

        return 0;
    }

    public List<ShipPosition> obtenerPosicionesParaBackend(String realIdBarco5,
                                                           String realIdBarco3,
                                                           String realIdBarco1) {
        List<ShipPosition> posiciones = new ArrayList<>();

        for (int idInterno = 1; idInterno < idBarcoActual; idInterno++) {
            List<Integer> casillas = new ArrayList<>();
            for (int i = 0; i < 225; i++) {
                if (tablero[i] == idInterno) casillas.add(i);
            }

            if (casillas.isEmpty()) continue;

            int tamano = casillas.size();
            String realId = (tamano >= 4) ? realIdBarco5 : (tamano >= 2 ? realIdBarco3 : realIdBarco1);
            if (realId == null || realId.isEmpty()) continue;

            String orientacion = getOrientacionBarco(idInterno);

            int centroIndex = tamano / 2;
            int posCentro = casillas.get(centroIndex);

            int xCentro = posCentro % 15;
            int yCentroApp = posCentro / 15;
            int yCentroBackend = 14 - yCentroApp;

            posiciones.add(new ShipPosition(
                    realId,
                    new Position(xCentro, yCentroBackend),
                    orientacion
            ));
        }

        return posiciones;
    }

    public void cargarBarcoDesdeServidor(String realId,
                                         int xCentro,
                                         int yServidor,
                                         String orientacion,
                                         String id5,
                                         String id3,
                                         String id1) {
        int tamano = (realId.equals(id5)) ? 5 : (realId.equals(id3) ? 3 : (realId.equals(id1) ? 1 : 0));
        if (tamano <= 0) return;

        int yCentroApp = 14 - yServidor;

        boolean horizontal = orientacion != null &&
                (orientacion.equalsIgnoreCase("E") || orientacion.equalsIgnoreCase("W"));

        int offset = tamano / 2;

        orientacionPorBarco.put(idBarcoActual, orientacion != null ? orientacion.toUpperCase() : "N");
        rotacionPorBarco.put(idBarcoActual, convertirOrientacionAGrados(getOrientacionBarco(idBarcoActual)));

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