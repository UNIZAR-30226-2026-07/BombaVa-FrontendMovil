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

        sincronizarTorpedosVisuales(gestor);

        // 3. Notificamos al adapter
        adapter.notifyDataSetChanged();
    }

    public void repaintPositions(LinkedHashSet<Integer> posiciones,
                                 GestorJuego gestor,
                                 String idBarcoSeleccionado,
                                 LinkedHashSet<Integer> posicionesRangoActual) {
        if (posiciones.isEmpty()) return;

        for (Integer pos : posiciones) {
            if (pos >= 0 && pos < matriz.size()) {
                matriz.get(pos).resetVisual();
            }
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
            if (pos >= 0 && pos < matriz.size()) {
                matriz.get(pos).setEnRangoAtaque(posicionesRangoActual.contains(pos));
            }
        }

        for (Integer pos : posiciones) {
            if (pos >= 0 && pos < matriz.size()) {
                adapter.notifyItemChanged(pos);
            }
        }
    }

    public void repaintDiffFlotas(List<BarcoLogico> flotaAnterior,
                                  List<BarcoLogico> flotaNueva,
                                  GestorJuego gestor,
                                  String idBarcoSeleccionado,
                                  LinkedHashSet<Integer> posicionesRangoActual) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();

        java.util.Map<String, BarcoLogico> mapaAnterior = new java.util.HashMap<>();
        java.util.Map<String, BarcoLogico> mapaNueva = new java.util.HashMap<>();

        if (flotaAnterior != null) {
            for (BarcoLogico b : flotaAnterior) {
                mapaAnterior.put(b.id, b);
            }
        }

        if (flotaNueva != null) {
            for (BarcoLogico b : flotaNueva) {
                mapaNueva.put(b.id, b);
            }
        }

        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        ids.addAll(mapaAnterior.keySet());
        ids.addAll(mapaNueva.keySet());

        for (String id : ids) {
            BarcoLogico anterior = mapaAnterior.get(id);
            BarcoLogico nuevo = mapaNueva.get(id);

            boolean haCambiado = false;

            if (anterior == null || nuevo == null) {
                haCambiado = true;
            } else {
                haCambiado =
                        anterior.x != nuevo.x ||
                                anterior.y != nuevo.y ||
                                !java.util.Objects.equals(anterior.orientation, nuevo.orientation) ||
                                anterior.hpActual != nuevo.hpActual ||
                                anterior.hpMax != nuevo.hpMax ||
                                anterior.tipo != nuevo.tipo ||
                                anterior.esAliado != nuevo.esAliado;
            }

            if (!haCambiado) continue;

            if (anterior != null) {
                posiciones.addAll(posicionesPara(anterior.x, anterior.y, anterior.orientation, anterior.tipo, gestor));
            }

            if (nuevo != null) {
                posiciones.addAll(posicionesPara(nuevo.x, nuevo.y, nuevo.orientation, nuevo.tipo, gestor));
            }
        }

        posiciones.addAll(posicionesRangoActual);

        if (!posiciones.isEmpty()) {
            repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);
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

    public void sincronizarTorpedosVisuales(GestorJuego gestor) {
        if (gestor == null) return;

        // 1. Limpiar agua
        for (Casilla c : matriz) {
            c.setTieneTorpedo(false, "N", true);
        }

        // 2. Pintar torpedos
        for (TorpedoLogico t : gestor.torpedosActivos) {
            int filaVisual = gestor.filaVisualDesdeLogica(t.y);

            // Si el tablero está dado la vuelta,
            // tenemos que girar el "Sprite" (dibujo) del torpedo para que no vuele hacia atrás.
            String direccionVisual = t.direccion;
            if (gestor.isPerspectivaInvertida()) {
                if ("N".equals(t.direccion)) direccionVisual = "S";
                else if ("S".equals(t.direccion)) direccionVisual = "N";
            }

            android.util.Log.d("DEBUG_TORPEDO_VISUAL", "🎨 Pintando en Pantalla -> Columna: " + t.x + ", Fila Visual: " + filaVisual + " | Apuntando hacia: " + direccionVisual);

            if (filaVisual >= 0 && filaVisual < 15 && t.x >= 0 && t.x < 15) {
                int posicionArray = filaVisual * 15 + t.x;
                matriz.get(posicionArray).setTieneTorpedo(true, direccionVisual, t.esAliado);
            }
        }
    }
}