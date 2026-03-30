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

        for (Integer pos : posicionesRangoActual) {
            if (pos >= 0 && pos < matriz.size()) {
                matriz.get(pos).setEnRangoAtaque(true);
            }
        }

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
        if (posiciones.isEmpty()) return;

        for (Integer pos : posiciones) {
            matriz.get(pos).resetVisual();
        }

        if (gestor != null) {
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

                    if (fila < 0 || fila >= 15 || col < 0 || col >= 15) continue;

                    int pos = fila * 15 + col;
                    if (!posiciones.contains(pos)) continue;

                    Casilla cas = matriz.get(pos);
                    cas.setTieneBarco(true);
                    cas.setIdBarcoStr(b.id);
                    cas.setEsAliado(b.esAliado);
                    cas.setTipoBarco(b.tipo);
                    cas.setDireccion(dir);
                    cas.setEsProa(celdaBarco[2] == 1);
                    cas.setIndiceEnBarco(celdaBarco[3]);
                    cas.setSlug(b.slug);
                    cas.setVidaActual(b.hpActual);
                    cas.setVidaMax(b.hpMax);
                    cas.setSeleccionado(esSel);
                }
            }
        }

        for (Integer pos : posiciones) {
            matriz.get(pos).setEnRangoAtaque(posicionesRangoActual.contains(pos));
        }

        for (Integer pos : posiciones) {
            adapter.notifyItemChanged(pos);
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