package com.example.bombavafrontmovil;

public class Casilla {
    private int fila, columna;
    private boolean seleccionado = false;

    // Datos del barco
    private int tipoBarco = 0;
    private int idBarco = -1;
    private boolean esAliado = true;

    // IMPORTANTE: Variable nueva para la rotación de la imagen
    private int direccion = 0; // 0=Norte, 1=Este, 2=Sur, 3=Oeste

    // Datos visuales de la parte del barco
    private boolean esProa = false; // Identifica la parte delantera
    private int indiceEnBarco = 0;  // Identifica si es medio o popa

    // Salud
    private int vidaCelda = 3; // Vida de esta casilla específica
    private int vidaActual, vidaMax; // Vida general (la que tenías tú)

    public Casilla(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    // --- GETTERS Y SETTERS ---

    public int getFila() { return fila; }
    public int getColumna() { return columna; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public int getTipoBarco() { return tipoBarco; }
    public void setTipoBarco(int tipoBarco) {
        this.tipoBarco = tipoBarco;
        // Mantenemos tu lógica de vida global
        this.vidaMax = tipoBarco;
        this.vidaActual = tipoBarco;
    }

    public int getIdBarco() { return idBarco; }
    public void setIdBarco(int idBarco) { this.idBarco = idBarco; }

    public boolean isTieneBarco() { return tipoBarco > 0; }

    // --- ESTO ES LO QUE ARREGLA EL ERROR DEL ADAPTADOR ---
    public int getDireccion() { return direccion; }
    public void setDireccion(int direccion) { this.direccion = direccion; }
    // -----------------------------------------------------

    public boolean isEsAliado() { return esAliado; }
    public void setEsAliado(boolean esAliado) { this.esAliado = esAliado; }

    public int getVidaCelda() { return vidaCelda; }
    public void setVidaCelda(int vidaCelda) { this.vidaCelda = vidaCelda; }

    public int getIndiceEnBarco() { return indiceEnBarco; }
    public void setIndiceEnBarco(int indiceEnBarco) { this.indiceEnBarco = indiceEnBarco; }

    public boolean isEsProa() { return esProa; }
    public void setEsProa(boolean esProa) { this.esProa = esProa; }

    // Tus getters/setters originales de vida global
    public int getVidaActual() { return vidaActual; }
    public void setVidaActual(int vidaActual) { this.vidaActual = vidaActual; }
    public int getVidaMax() { return vidaMax; }
}