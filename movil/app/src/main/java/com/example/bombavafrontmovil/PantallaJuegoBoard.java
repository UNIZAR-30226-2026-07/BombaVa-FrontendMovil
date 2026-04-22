package com.example.bombavafrontmovil;

import android.widget.ImageView;

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

        // Proyectiles SIEMPRE después de barcos para que se pinten encima
        sincronizarTorpedosVisuales(gestor);

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
            for (BarcoLogico b : flotaAnterior) mapaAnterior.put(b.id, b);
        }
        if (flotaNueva != null) {
            for (BarcoLogico b : flotaNueva) mapaNueva.put(b.id, b);
        }

        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        ids.addAll(mapaAnterior.keySet());
        ids.addAll(mapaNueva.keySet());

        for (String id : ids) {
            BarcoLogico anterior = mapaAnterior.get(id);
            BarcoLogico nuevo = mapaNueva.get(id);

            boolean haCambiado = anterior == null || nuevo == null
                    || anterior.x != nuevo.x
                    || anterior.y != nuevo.y
                    || !java.util.Objects.equals(anterior.orientation, nuevo.orientation)
                    || anterior.hpActual != nuevo.hpActual
                    || anterior.hpMax != nuevo.hpMax
                    || anterior.tipo != nuevo.tipo
                    || anterior.esAliado != nuevo.esAliado;

            if (!haCambiado) continue;

            if (anterior != null) posiciones.addAll(posicionesPara(anterior.x, anterior.y, anterior.orientation, anterior.tipo, gestor));
            if (nuevo != null)    posiciones.addAll(posicionesPara(nuevo.x, nuevo.y, nuevo.orientation, nuevo.tipo, gestor));
        }

        posiciones.addAll(posicionesRangoActual);

        if (!posiciones.isEmpty()) {
            repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);
        }
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
            int fila = (gestor != null) ? gestor.filaVisualDesdeLogica(filaLogica) : (14 - filaLogica);

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

    /**
     * Pinta todos los proyectiles activos sobre el tablero.
     *
     * FUENTE DE VERDAD: las coordenadas x,y de cada TorpedoLogico son ABSOLUTAS
     * del mapa del servidor (igual que las de los barcos). Se convierten a fila
     * visual con filaVisualDesdeLogica(), exactamente igual que se hace con los barcos.
     *
     * La dirección visual se deriva del vector absoluto del proyectil y luego se
     * invierte si el jugador ve el tablero en perspectiva invertida (jugador NORTH
     * que ve desde arriba → su "avanzar" es hacia filas visuales crecientes).
     */
    public void sincronizarTorpedosVisuales(GestorJuego gestor) {
        if (gestor == null) return;

        // Limpieza total: resetVisual() ya limpió torpedo/mina, pero por si
        // se llama a este método de forma aislada, limpiamos explícitamente.
        for (Casilla c : matriz) {
            c.setTieneTorpedo(false, "N", true);
            c.setTieneMina(false, false);
        }

        boolean perspInvertida = gestor.isPerspectivaInvertida();
        java.util.Set<Integer> posicionesOcupadas = new java.util.HashSet<>();

        for (TorpedoLogico p : gestor.torpedosActivos) {

            // ── Posición visual ──────────────────────────────────────────────
            // Las coordenadas x,y del torpedo son absolutas del servidor,
            // igual que las de los barcos. Usamos la misma conversión.
            int filaVisual = gestor.filaVisualDesdeLogica(p.y);
            int col = p.x;

            if (filaVisual < 0 || filaVisual >= 15 || col < 0 || col >= 15) continue;

            int pos = filaVisual * 15 + col;
            if (posicionesOcupadas.contains(pos)) continue;
            posicionesOcupadas.add(pos);

            Casilla c = matriz.get(pos);

            if ("MINE".equals(p.tipo)) {
                c.setTieneMina(true, p.esAliado);
                android.util.Log.d("DEBUG_MINA", "Pintando MINA en (" + col + "," + filaVisual + ") aliada=" + p.esAliado);

            } else {
                // ── Dirección visual del torpedo ─────────────────────────────
                // El vector (vectorX, vectorY) es ABSOLUTO del servidor.
                // Convertimos el vector absoluto a string de dirección visual.
                // Si la perspectiva está invertida (jugador NORTH), tanto el eje
                // vertical como el horizontal se invierten en pantalla, por lo
                // que invertimos los dos componentes del vector.
                int vx = p.vectorX;
                int vy = p.vectorY;

                if (perspInvertida) {
                    vx = -vx;
                    vy = -vy;
                }

                String dirVisual = vectorADireccion(vx, vy);
                c.setTieneTorpedo(true, dirVisual, p.esAliado);

                android.util.Log.d("DEBUG_TORPEDO_DIR",
                        "Torpedo " + p.id.substring(0, Math.min(4, p.id.length()))
                                + " vector abs=(" + p.vectorX + "," + p.vectorY + ")"
                                + " perspInv=" + perspInvertida
                                + " → dirVisual=" + dirVisual
                                + " pos=(" + col + "," + filaVisual + ")");
            }
        }
    }

    /**
     * Convierte un vector de movimiento (absoluto o ya corregido por perspectiva)
     * a la cadena de dirección que usa Casilla/BoardAdapter.
     *
     * El icono ic_torpedo apunta hacia ARRIBA (Norte) con rotación 0°.
     * BoardAdapter aplica: N→0°, S→180°, E→90°, W→270°.
     *
     * Tabla de vectores del servidor:
     *   vectorY=-1 → el torpedo sube en coords servidor  → visualmente Norte  → "N"
     *   vectorY=+1 → el torpedo baja en coords servidor  → visualmente Sur    → "S"
     *   vectorX=+1 → el torpedo va a la derecha          → visualmente Este   → "E"
     *   vectorX=-1 → el torpedo va a la izquierda        → visualmente Oeste  → "W"
     */
    private String vectorADireccion(int vx, int vy) {
        if (vy < 0) return "N";
        if (vy > 0) return "S";
        if (vx > 0) return "E";
        if (vx < 0) return "W";
        return "N"; // fallback
    }
}