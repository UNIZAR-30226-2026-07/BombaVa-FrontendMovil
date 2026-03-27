package com.example.bombavafrontmovil;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de Lógica de Negocio.
 * Gestiona el tablero virtual, colisiones y equipamiento de armas.
 * NO sabe nada de la Interfaz de Usuario (botones, colores, etc).
 */
public class GestorConfiguracionFlota {

    private int[] casillasOcupadas = new int[225];
    private Map<Integer, String> armasEquipadasPorBarco = new HashMap<>();
    private int idBarcoActual = 1;

    // 1. AJUSTE DE POSICIÓN (Evita que el barco se salga del mapa)
    public int ajustarPosicion(int pos, int tamano, boolean enHorizontal) {
        int fila = pos / 15;
        int col = pos % 15;

        if (enHorizontal && (col + tamano > 15)) col = 15 - tamano;
        if (!enHorizontal && (fila + tamano > 15)) fila = 15 - tamano;

        return (fila * 15) + col;
    }

    // 2. VALIDACIÓN (0 = OK, 1 = Zona Invalida, 2 = Colisión)
    public int validarColocacion(int posAjustada, int tamano, boolean enHorizontal) {
        int fila = posAjustada / 15;

        if (fila < 10) return 1; // Zona enemiga o neutral restringida

        for (int i = 0; i < tamano; i++) {
            int check = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            if (casillasOcupadas[check] != 0) return 2; // Choca con otro barco
        }
        return 0; // Todo perfecto
    }

    // 3. COLOCAR Y BORRAR
    public void colocarBarco(int posAjustada, int tamano, boolean enHorizontal) {
        for (int i = 0; i < tamano; i++) {
            int p = enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15));
            casillasOcupadas[p] = idBarcoActual;
        }
        idBarcoActual++;
    }


    // 4. CONSULTAS DE ESTADO DEL TABLERO
    public int getIdBarcoEn(int pos) {
        return casillasOcupadas[pos];
    }

    public int getTamanoBarco(int idBarco) {
        int count = 0;
        for (int id : casillasOcupadas) if (id == idBarco) count++;
        return count;
    }

    public boolean estaBarcoColocado(int tamanoBuscado) {
        for (int id : casillasOcupadas) {
            if (id != 0 && getTamanoBarco(id) == tamanoBuscado) return true;
        }
        return false;
    }

    // ... Todo el código anterior (ajustarPosicion, validarColocacion, etc) se queda igual ...

    // Usa un Set para evitar armas duplicadas en el mismo barco
    private Map<Integer, java.util.Set<String>> armasPorBarco = new HashMap<>();

    // Modificamos borrar barco para que use la nueva variable
    public void borrarBarco(int idBarco) {
        for (int i = 0; i < 225; i++) {
            if (casillasOcupadas[i] == idBarco) casillasOcupadas[i] = 0;
        }
        armasPorBarco.remove(idBarco);
    }

    // 5. GESTIÓN DE INVENTARIO DE ARMAS
    public void equiparArma(int idBarco, String arma) {
        // Si el barco no tiene lista de armas aún, se la creamos
        armasPorBarco.putIfAbsent(idBarco, new java.util.HashSet<>());
        // Añadimos el arma a su inventario
        armasPorBarco.get(idBarco).add(arma);
    }

    public boolean tieneArmaEquipada(int idBarco, String arma) {
        return armasPorBarco.containsKey(idBarco) && armasPorBarco.get(idBarco).contains(arma);
    }

    // Valida que estén todas equipadas: 3 (grande) + 2 (mediano) + 1 (pequeño) = 6 armas
    public boolean configuracionArmasCompleta() {
        int totalArmas = 0;
        for (java.util.Set<String> armas : armasPorBarco.values()) {
            totalArmas += armas.size();
        }
        return totalArmas == 6;
    }

    public Map<Integer, java.util.Set<String>> getArmasEquipadas() {
        return armasPorBarco;
    }
}