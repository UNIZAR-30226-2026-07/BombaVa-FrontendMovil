package com.example.bombavafrontmovil;

public class Casilla {
    private int fila, columna;
    private boolean seleccionado = false;

    // Estado del barco
    private boolean tieneBarco = false;
    private int idBarco = -1;       // ID antiguo (int) - Lo mantenemos por compatibilidad
    private String idBarcoStr = null; // ID nuevo (UUID del servidor)
    private int tipoBarco = 0;      // Tamaño
    private boolean esAliado = true;

    // Rotación y visualización
    private int direccion = 0; // 0=Norte, 1=Este, 2=Sur, 3=Oeste
    private boolean esProa = false;
    private int indiceEnBarco = 0;

    // Vida
    private int vidaCelda = 3;
    private int vidaActual, vidaMax;

    public Casilla(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    // --- GETTERS Y SETTERS ---

    public int getFila() { return fila; }
    public int getColumna() { return columna; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public boolean isTieneBarco() { return tieneBarco; }
    public void setTieneBarco(boolean tieneBarco) { this.tieneBarco = tieneBarco; }

    // ID numérico (Legacy)
    public int getIdBarco() { return idBarco; }
    public void setIdBarco(int idBarco) {
        this.idBarco = idBarco;
        if (idBarco != -1) this.tieneBarco = true;
    }

    // ID Texto (Nuevo Backend)
    public String getIdBarcoStr() { return idBarcoStr; }
    public void setIdBarcoStr(String idBarcoStr) {
        this.idBarcoStr = idBarcoStr;
        // Si el ID no es nulo, significa que hay barco
        this.tieneBarco = (idBarcoStr != null);
    }

    public int getTipoBarco() { return tipoBarco; }
    public void setTipoBarco(int tipoBarco) {
        this.tipoBarco = tipoBarco;
        this.vidaMax = tipoBarco;
        this.vidaActual = tipoBarco;
    }

    public int getDireccion() { return direccion; }
    public void setDireccion(int direccion) { this.direccion = direccion; }

    public boolean isEsAliado() { return esAliado; }
    public void setEsAliado(boolean esAliado) { this.esAliado = esAliado; }

    public int getVidaCelda() { return vidaCelda; }
    public void setVidaCelda(int vidaCelda) { this.vidaCelda = vidaCelda; }

    public int getIndiceEnBarco() { return indiceEnBarco; }
    public void setIndiceEnBarco(int indiceEnBarco) { this.indiceEnBarco = indiceEnBarco; }

    public boolean isEsProa() { return esProa; }
    public void setEsProa(boolean esProa) { this.esProa = esProa; }

    public int getVidaActual() { return vidaActual; }
    public void setVidaActual(int vidaActual) { this.vidaActual = vidaActual; }
    public int getVidaMax() { return vidaMax; }
}