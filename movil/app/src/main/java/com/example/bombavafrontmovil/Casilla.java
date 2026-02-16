package com.example.bombavafrontmovil;
//prueba
public class Casilla {
    private int fila, columna;
    private boolean seleccionado = false;
    private int tipoBarco = 0; // 0:Agua, 1, 3, 5
    private int idBarco = -1;  // ID para agrupar celdas
    private int vidaActual, vidaMax;

    public Casilla(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    // Getters y Setters
    public int getFila() { return fila; }
    public int getColumna() { return columna; }
    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public int getTipoBarco() { return tipoBarco; }
    public void setTipoBarco(int tipoBarco) {
        this.tipoBarco = tipoBarco;
        this.vidaMax = tipoBarco; // La vida inicial es igual al tamaño
        this.vidaActual = tipoBarco;
    }

    public int getIdBarco() { return idBarco; }
    public void setIdBarco(int idBarco) { this.idBarco = idBarco; }
    public boolean isTieneBarco() { return tipoBarco > 0; }
    public int getVidaActual() { return vidaActual; }
    public int getVidaMax() { return vidaMax; }
}