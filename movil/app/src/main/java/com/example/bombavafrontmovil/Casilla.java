package com.example.bombavafrontmovil;

public class Casilla {
    private int fila, columna;
    private boolean seleccionado = false;
    private int tipoBarco = 0;
    private int idBarco = -1;
    private int vidaActual, vidaMax;
    private boolean esProa = false; // NUEVO: Identifica la parte delantera

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
        this.vidaMax = tipoBarco;
        this.vidaActual = tipoBarco;
    }

    public int getIdBarco() { return idBarco; }
    public void setIdBarco(int idBarco) { this.idBarco = idBarco; }
    public boolean isTieneBarco() { return tipoBarco > 0; }
    public int getVidaActual() { return vidaActual; }
    public void setVidaActual(int vidaActual) { this.vidaActual = vidaActual; } // Añadido
    public int getVidaMax() { return vidaMax; }

    public boolean isEsProa() { return esProa; }
    public void setEsProa(boolean esProa) { this.esProa = esProa; }
}