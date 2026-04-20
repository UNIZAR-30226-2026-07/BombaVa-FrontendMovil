package com.example.bombavafrontmovil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PantallaJuegoBoard {

    private final List<Casilla> matriz = new ArrayList<>();
    private final BoardAdapter adapter;

    public interface OnCasillaClick {
        void onClick(Casilla casilla);
    }

    public PantallaJuegoBoard(AppCompatActivity activity,
                              OnCasillaClick listener) {
        for (int i = 0; i < 225; i++) {
            matriz.add(new Casilla(i / 15, i % 15));
        }

        RecyclerView rv = activity.findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(activity, 15));
        adapter = new BoardAdapter(matriz, listener::onClick);
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);
        rv.setHasFixedSize(true);
    }

    public List<Casilla> getMatriz() {
        return matriz;
    }

    public void repaintFull(GestorJuego gestor,
                            String idBarcoSeleccionado,
                            LinkedHashSet<Integer> posicionesRangoActual) {
        if (gestor == null) return;

        List<Casilla> snapshotAnterior = new ArrayList<>(matriz.size());
        for (Casilla c : matriz) snapshotAnterior.add(c.clonar());

        for (Casilla c : matriz) c.resetVisual();

        // 1. Pintamos todos los barcos en bruto
        for (BarcoLogico b : gestor.getFlota()) {
            boolean esSel = idBarcoSeleccionado != null
                    && idBarcoSeleccionado.equals(b.id)
                    && b.esAliado;

            int dir = orientacionADireccionVisual(b.orientation);
            List<int[]> celdasRender = BarcoLogico.getCeldasPara(b.x, b.y, b.orientation, b.tipo);

            for (int[] celdaBarco : celdasRender) {
                int filaLogica = celdaBarco[0];
                int col = celdaBarco[1];
                int fila = gestor.filaVisualDesdeLogica(filaLogica);

                boolean esProa = celdaBarco[2] == 1;
                int indice = celdaBarco[3];

                if (fila < 0 || fila >= 15 || col < 0 || col >= 15) continue;

                Casilla cas = matriz.get(fila * 15 + col);
                cas.setTieneBarco(true);
                cas.setIdBarcoStr(b.id);
                cas.setEsAliado(b.esAliado);
                cas.setTipoBarco(b.tipo);
                cas.setDireccion(dir);
                cas.setEsProa(esProa);
                cas.setIndiceEnBarco(indice);
                cas.setSlug(b.slug);
                cas.setVidaActual(b.hpActual);
                cas.setVidaMax(b.hpMax);
                cas.setSeleccionado(esSel);
            }
        }

        // 2. Aplicamos niebla de guerra
        aplicarNieblaDeGuerra(gestor);

        // 3. Pintamos rango de ataque
        for (Integer pos : posicionesRangoActual) {
            if (pos >= 0 && pos < matriz.size()) {
                matriz.get(pos).setEnRangoAtaque(true);
            }
        }

        // 4. Notificamos solo cambios
        for (int i = 0; i < matriz.size(); i++) {
            if (!matriz.get(i).equivaleA(snapshotAnterior.get(i))) {
                adapter.notifyItemChanged(i);
            }
        }
    }

    public void repaintPositions(LinkedHashSet<Integer> posiciones,
                                 GestorJuego gestor,
                                 String idBarcoSeleccionado,
                                 LinkedHashSet<Integer> posicionesRangoActual) {
        // Con niebla de guerra conviene recalcular todo el tablero
        repaintFull(gestor, idBarcoSeleccionado, posicionesRangoActual);
    }

    public void repaintDiffFlotas(List<BarcoLogico> flotaAnterior,
                                  List<BarcoLogico> flotaNueva,
                                  GestorJuego gestor,
                                  String idBarcoSeleccionado,
                                  LinkedHashSet<Integer> posicionesRangoActual) {
        // Con niebla de guerra conviene recalcular todo el tablero
        repaintFull(gestor, idBarcoSeleccionado, posicionesRangoActual);
    }

    private void aplicarNieblaDeGuerra(GestorJuego gestor) {
        // 1. Todo invisible por defecto
        for (Casilla c : matriz) {
            c.setVisible(false);
        }

        // 2. Mis barcos y su rango Manhattan generan visión
        for (BarcoLogico b : gestor.getFlota()) {
            if (!b.esAliado) continue;

            List<int[]> celdasAliadas = BarcoLogico.getCeldasPara(b.x, b.y, b.orientation, b.tipo);
            int rangoVision = b.getRangoVision();

            // Las propias celdas del barco siempre visibles
            for (int[] celdaBarco : celdasAliadas) {
                int filaLogica = celdaBarco[0];
                int col = celdaBarco[1];
                int filaVisual = gestor.filaVisualDesdeLogica(filaLogica);

                if (filaVisual < 0 || filaVisual >= 15 || col < 0 || col >= 15) continue;
                matriz.get(filaVisual * 15 + col).setVisible(true);
            }

            // Campo de visión Manhattan desde cada celda del barco
            for (Casilla casilla : matriz) {
                int xObjetivo = casilla.getColumna();
                int yObjetivoLogica = gestor.filaLogicaDesdeVisual(casilla.getFila());

                boolean visible = false;
                for (int[] origen : celdasAliadas) {
                    int yOrigen = origen[0];
                    int xOrigen = origen[1];

                    int dist = Math.abs(xObjetivo - xOrigen) + Math.abs(yObjetivoLogica - yOrigen);
                    if (dist <= rangoVision) {
                        visible = true;
                        break;
                    }
                }

                if (visible) {
                    casilla.setVisible(true);
                }
            }
        }

        // 3. Ocultamos barcos enemigos en casillas no visibles
        for (Casilla c : matriz) {
            if (c.isTieneBarco() && !c.isEsAliado() && !c.isVisible()) {
                c.setTieneBarco(false);
                c.setIdBarco(-1);
                c.setIdBarcoStr(null);
                c.setTipoBarco(0);
                c.setDireccion(0);
                c.setEsProa(false);
                c.setIndiceEnBarco(0);
                c.setSlug(null);
                c.setVidaActual(0);
                c.setVidaMax(0);
                c.setSeleccionado(false);
            }
        }
    }

    public LinkedHashSet<Integer> posicionesDeBarco(BarcoLogico barco) {
        return posicionesPara(barco.x, barco.y, barco.orientation, barco.tipo, null);
    }

    public LinkedHashSet<Integer> posicionesPara(int x, int y, String orientation, int tipo) {
        return posicionesPara(x, y, orientation, tipo, null);
    }

    public LinkedHashSet<Integer> posicionesPara(int x, int y, String orientation, int tipo, GestorJuego gestor) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        List<int[]> celdas = BarcoLogico.getCeldasPara(x, y, orientation, tipo);

        for (int[] celda : celdas) {
            int filaLogica = celda[0];
            int col = celda[1];

            int fila = (gestor != null)
                    ? gestor.filaVisualDesdeLogica(filaLogica)
                    : (14 - filaLogica);

            if (fila < 0 || fila >= 15 || col < 0 || col >= 15) continue;
            posiciones.add(fila * 15 + col);
        }
        return posiciones;
    }

    private int orientacionADireccionVisual(String orientation) {
        if ("N".equals(orientation)) return 2;
        if ("E".equals(orientation)) return 3;
        if ("S".equals(orientation)) return 0;
        if ("W".equals(orientation)) return 1;
        return 2;
    }
}