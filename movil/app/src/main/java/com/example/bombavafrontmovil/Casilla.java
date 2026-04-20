package com.example.bombavafrontmovil;

import java.util.Objects;

public class Casilla {
    private int fila, columna;
    private boolean seleccionado = false;
    private boolean enRangoAtaque = false;

    private boolean tieneBarco = false;
    private int idBarco = -1;
    private String idBarcoStr = null;
    private int tipoBarco = 0;
    private boolean esAliado = true;

    private int direccion = 0; // 0=Norte, 1=Este, 2=Sur, 3=Oeste
    private boolean esProa = false;
    private int indiceEnBarco = 0;

    private int vidaCelda = 3;
    private String slug;
    private int vidaActual, vidaMax;
    private boolean tieneTorpedo = false;
    private String direccionTorpedo = "N";
    private boolean esTorpedoAliado = true;

    public Casilla(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() { return fila; }
    public int getColumna() { return columna; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public boolean isEnRangoAtaque() { return enRangoAtaque; }
    public void setEnRangoAtaque(boolean enRangoAtaque) { this.enRangoAtaque = enRangoAtaque; }

    public boolean isTieneBarco() { return tieneBarco; }
    public void setTieneBarco(boolean tieneBarco) { this.tieneBarco = tieneBarco; }

    public int getIdBarco() { return idBarco; }
    public void setIdBarco(int idBarco) {
        this.idBarco = idBarco;
        if (idBarco != -1) this.tieneBarco = true;
    }

    public String getIdBarcoStr() { return idBarcoStr; }
    public void setIdBarcoStr(String idBarcoStr) {
        this.idBarcoStr = idBarcoStr;
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
    public void setVidaMax(int vidaMax) { this.vidaMax = vidaMax; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean hasTorpedo() { return tieneTorpedo; }
    public void setTieneTorpedo(boolean tiene, String direccion, boolean esAliado) {
        this.tieneTorpedo = tiene;
        this.direccionTorpedo = direccion;
        this.esTorpedoAliado = esAliado;
    }
    public String getDireccionTorpedo() { return direccionTorpedo; }
    public boolean isTorpedoAliado() { return esTorpedoAliado; }

    public void resetVisual() {
        seleccionado = false;
        enRangoAtaque = false;
        tieneBarco = false;
        idBarco = -1;
        idBarcoStr = null;
        tipoBarco = 0;
        esAliado = true;
        direccion = 0;
        esProa = false;
        indiceEnBarco = 0;
        vidaCelda = 3;
        slug = null;
        vidaActual = 0;
        vidaMax = 0;
    }

    public Casilla clonar() {
        Casilla c = new Casilla(fila, columna);
        c.seleccionado = seleccionado;
        c.enRangoAtaque = enRangoAtaque;
        c.tieneBarco = tieneBarco;
        c.idBarco = idBarco;
        c.idBarcoStr = idBarcoStr;
        c.tipoBarco = tipoBarco;
        c.esAliado = esAliado;
        c.direccion = direccion;
        c.esProa = esProa;
        c.indiceEnBarco = indiceEnBarco;
        c.vidaCelda = vidaCelda;
        c.slug = slug;
        c.vidaActual = vidaActual;
        c.vidaMax = vidaMax;
        c.tieneTorpedo = tieneTorpedo;
        c.direccionTorpedo = direccionTorpedo;
        c.esTorpedoAliado = esTorpedoAliado;
        return c;
    }

    public boolean equivaleA(Casilla otra) {
        if (otra == null) return false;

        return fila == otra.fila
                && columna == otra.columna
                && seleccionado == otra.seleccionado
                && enRangoAtaque == otra.enRangoAtaque
                && tieneBarco == otra.tieneBarco
                && idBarco == otra.idBarco
                && Objects.equals(idBarcoStr, otra.idBarcoStr)
                && tipoBarco == otra.tipoBarco
                && esAliado == otra.esAliado
                && direccion == otra.direccion
                && esProa == otra.esProa
                && indiceEnBarco == otra.indiceEnBarco
                && vidaCelda == otra.vidaCelda
                && vidaActual == otra.vidaActual
                && vidaMax == otra.vidaMax
                && Objects.equals(slug, otra.slug)
                && tieneTorpedo == otra.tieneTorpedo
                && Objects.equals(direccionTorpedo, otra.direccionTorpedo)
                && esTorpedoAliado == otra.esTorpedoAliado;
    }
}