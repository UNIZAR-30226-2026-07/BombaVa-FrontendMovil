package com.example.bombavafrontmovil;

import android.util.Log;

import com.example.bombavafrontmovil.models.UserShip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;

public class GestorJuego {
    final Socket socket;
    final List<BarcoLogico> flota = new ArrayList<>();
    final String matchId;
    final String myUserId;
    final PartidaListener listener;
    final Map<String, UserShip> diccionarioFlota;
    final Map<String, UserShip> inventarioOriginal;
    final List<TorpedoLogico> torpedosActivos = new ArrayList<>();

    boolean esMiTurno = false;

    final GestorJuegoMapper mapper;
    final GestorJuegoSocketBinder socketBinder;

    public interface PartidaListener {
        void onSnapshotCompleto();
        void onRecursosActualizados(int fuel, int ammo);
        void onPartidaTerminada(String ganadorId, String razon);
        void onErrorJuego(String mensaje);
        void onBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo);
        void onBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo);
        void onVisionUpdateParcial(List<BarcoLogico> flotaAnterior, List<BarcoLogico> flotaNueva);
    }

    public GestorJuego(Socket socket,
                       String matchId,
                       String userId,
                       Map<String, UserShip> diccionarioFlota,
                       PartidaListener listener) {
        this.socket = socket;
        this.matchId = matchId;
        this.myUserId = userId;
        this.diccionarioFlota = diccionarioFlota;
        this.inventarioOriginal = new HashMap<>(diccionarioFlota);
        this.listener = listener;

        this.mapper = new GestorJuegoMapper(this);
        this.socketBinder = new GestorJuegoSocketBinder(this);

        socketBinder.configurarListeners();
        unirseAPartida();
    }

    public boolean isPerspectivaInvertida() {
        for (BarcoLogico b : flota) {
            if (b.esAliado) {
                return b.y < 7;
            }
        }
        return false;
    }

    public int filaVisualDesdeLogica(int filaLogica) {
        if (isPerspectivaInvertida()) {
            return 14 - filaLogica;
        }
        return filaLogica;
    }

    public int filaLogicaDesdeVisual(int filaVisual) {
        if (isPerspectivaInvertida()) {
            return 14 - filaVisual;
        }
        return filaVisual;
    }

    private void unirseAPartida() {
        try {
            socket.emit("game:join", matchId);
            Log.d("DEBUG_JOIN", "Emit game:join -> " + matchId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void procesarStartInfo(Object[] args) {
        mapper.procesarStartInfo(args);
    }

    public boolean isEsMiTurno() {
        return esMiTurno;
    }

    public List<BarcoLogico> getFlota() {
        return flota;
    }

    public BarcoLogico obtenerBarcoEn(int filaVisual, int columna) {
        for (BarcoLogico b : flota) {
            for (int[] c : b.getCeldas()) {
                int filaLogica = c[0];
                int col = c[1];
                int filaConvertida = filaVisualDesdeLogica(filaLogica);

                if (filaConvertida == filaVisual && col == columna) {
                    return b;
                }
            }
        }
        return null;
    }

    public BarcoLogico obtenerBarcoPorId(String idBarco) {
        for (BarcoLogico b : flota) {
            if (b.id.equals(idBarco)) return b;
        }
        return null;
    }

    public void moverBarco(String shipId, String direction) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);
            payload.put("direction", direction);
            socket.emit("ship:move", payload);
            Log.d("DEBUG_MOVE", "Emit ship:move -> " + payload.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rotarBarco(String shipId, int degrees) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);
            payload.put("degrees", degrees);
            socket.emit("ship:rotate", payload);
            Log.d("DEBUG_ROTATE", "Emit ship:rotate -> " + payload.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dispararCannon(String shipId, int filaVisual, int columna) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            JSONObject target = new JSONObject();
            target.put("x", columna);
            target.put("y", filaLogicaDesdeVisual(filaVisual));
            payload.put("target", target);

            Log.d("DEBUG_ATTACK", "🚀 [PRE-ATAQUE] Emitiendo ataque Cañon -> " + payload.toString());
            socket.emit("ship:attack:cannon", payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lanzarTorpedo(String shipId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            Log.d("DEBUG_TORPEDO", "📡 Solicitando permiso de disparo al Servidor...");
            socket.emit("ship:attack:torpedo", payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registra un torpedo/mina recibido desde el servidor.
     *
     * IMPORTANTE: los parámetros x, y, vectorX, vectorY son COORDENADAS ABSOLUTAS
     * del mapa del servidor. NO se traducen aquí. La conversión a coordenadas
     * visuales se hace en PantallaJuegoBoard.sincronizarTorpedosVisuales(),
     * igual que se hace con los barcos en repaintFull().
     *
     * El evento projectile:launched del servidor manda coordenadas ya traducidas
     * al bando del jugador que disparó. Eso significa que para el rival llegan
     * incorrectas. Por eso la fuente de verdad real son los datos de
     * match:vision_update → sincronizarProyectilesVision(), que sí llegan
     * correctamente a cada jugador. Este método se usa solo como registro rápido
     * para mostrar feedback inmediato al tirador; la posición definitiva siempre
     * se sobreescribe con el siguiente vision_update.
     */
    public void registrarTorpedoDesdeServer(String id, int x, int y,
                                            int vectorX, int vectorY,
                                            int lifeDistance, boolean esAliado,
                                            String tipo) {
        // Evitar duplicados si ya existe un proyectil con ese id
        for (TorpedoLogico t : torpedosActivos) {
            if (t.id.equals(id)) {
                t.x = x;
                t.y = y;
                t.vectorX = vectorX;
                t.vectorY = vectorY;

                t.lifeDistance = lifeDistance;
                t.esAliado = esAliado;
                t.tipo = tipo;

                // Actualizar dirección visual si el vector ha cambiado
                if (t.vectorY == -1) t.direccion = "N";
                else if (t.vectorY == 1) t.direccion = "S";
                else if (t.vectorX == 1) t.direccion = "E";
                else if (t.vectorX == -1) t.direccion = "W";

                if (listener != null) {
                    listener.onSnapshotCompleto();
                }
                return; // Salimos para no añadirlo dos veces
            }
        }

        TorpedoLogico nuevo = new TorpedoLogico(id, x, y, vectorX, vectorY, lifeDistance, esAliado, tipo);
        torpedosActivos.add(nuevo);
        Log.d("DEBUG_TORPEDO", "➕ Registrado proyectil " + tipo + " id=" + id.substring(0, Math.min(4, id.length()))
                + " pos=(" + x + "," + y + ") vec=(" + vectorX + "," + vectorY + ") aliado=" + esAliado);

        if (listener != null) {
            listener.onSnapshotCompleto();
        }
    }

    /**
     * Aplica el daño de un impacto, elimina el proyectil y hunde el barco si procede.
     */
    public void procesarImpactoTorpedo(String shipId, String proyectilId, int newHp) {
        // 1. Borrar el torpedo del agua
        TorpedoLogico torpedoRoto = null;
        for (TorpedoLogico t : torpedosActivos) {
            if (t.id.equals(proyectilId)) {
                torpedoRoto = t;
                break;
            }
        }
        if (torpedoRoto != null) torpedosActivos.remove(torpedoRoto);

        // 2. Bajar la vida del barco
        BarcoLogico barcoHundido = null;
        for (BarcoLogico b : flota) {
            if (b.id.equals(shipId)) {
                b.hpActual = newHp;
                Log.d("DEBUG_TORPEDO", "💥 Impacto en barco " + shipId + " → HP restante: " + newHp);
                if (b.hpActual <= 0) {
                    barcoHundido = b;
                    Log.d("DEBUG_TORPEDO", "🏴‍☠️ Barco " + shipId + " HUNDIDO.");
                }
                break;
            }
        }

        if (barcoHundido != null) {
            flota.remove(barcoHundido);
        }

        if (listener != null) {
            listener.onSnapshotCompleto();
        }
    }


    /**
     * Actualiza la posición de un torpedo existente según un evento del servidor.
     * Las coordenadas x, y recibidas son absolutas del mapa.
     */
    public void actualizarTorpedo(String id, String status, int x, int y, int life) {
        // Si el proyectil muere, el SocketBinder ya lo elimina de la lista antes de llamar aquí
        // o puedes gestionarlo aquí también.

        for (TorpedoLogico t : torpedosActivos) {
            if (t.id.equals(id)) {
                // Solo calculamos dirección si tenemos una posición previa válida
                if (x != -1 && y != -1) {
                    // Cálculo de dirección por comparación de movimiento
                    if (x > t.x) t.direccion = "E";
                    else if (x < t.x) t.direccion = "W";
                    else if (y > t.y) t.direccion = "S";
                    else if (y < t.y) t.direccion = "N";

                    t.x = x;
                    t.y = y;
                }
                if (life != -1) t.lifeDistance = life;
                return;
            }
        }
    }

    /**
     * Reemplaza la lista completa de proyectiles activos con los datos del servidor.
     * Llamado desde match:vision_update — esta es la FUENTE DE VERDAD de posiciones.
     * Las coordenadas que llegan aquí son absolutas del mapa del servidor.
     */
    public void sincronizarProyectilesVision(JSONArray proyPropios, JSONArray proyEnemigos) {
        // Guardamos el estado actual para NO perder la rotación de los misiles
        Map<String, TorpedoLogico> mapaAntiguos = new HashMap<>();
        for (TorpedoLogico t : torpedosActivos) {
            mapaAntiguos.put(t.id, t);
        }

        torpedosActivos.clear();

        try {
            procesarArrayVision(proyPropios, mapaAntiguos, true);
            procesarArrayVision(proyEnemigos, mapaAntiguos, false);

            Log.d("DEBUG_TORPEDO", "👁️ Visión sincronizada: " + torpedosActivos.size() + " proyectiles activos.");
        } catch (Exception e) {
            Log.e("DEBUG_TORPEDO", "Error procesando proyectiles de vision_update", e);
        }
    }

    // Metodo auxiliar para no repetir código
    private void procesarArrayVision(JSONArray array, Map<String, TorpedoLogico> mapaAntiguos, boolean esAliado) throws Exception {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject p = array.getJSONObject(i);
            String id = p.getString("id");
            int x = p.getInt("x");
            int y = p.getInt("y");
            int vx = p.optInt("vectorX", 0);
            int vy = p.optInt("vectorY", 0);
            int life = p.optInt("lifeDistance", 0);
            String tipo = p.optString("type", "TORPEDO");

            TorpedoLogico antiguo = mapaAntiguos.get(id);
            if (antiguo != null) {
                // Recuperar vector si el backend lo oculta por la niebla
                if (vx == 0 && vy == 0) {
                    vx = antiguo.vectorX;
                    vy = antiguo.vectorY;
                }

                // Truco del radar para la rotación matemática
                if (antiguo.x != x || antiguo.y != y) {
                    if (x > antiguo.x) antiguo.direccion = "E";
                    else if (x < antiguo.x) antiguo.direccion = "W";
                    else if (y > antiguo.y) antiguo.direccion = "S";
                    else if (y < antiguo.y) antiguo.direccion = "N";
                }

                antiguo.x = x;
                antiguo.y = y;
                antiguo.vectorX = vx;
                antiguo.vectorY = vy;
                antiguo.lifeDistance = life;
                torpedosActivos.add(antiguo);
            } else {
                torpedosActivos.add(new TorpedoLogico(id, x, y, vx, vy, life, esAliado, tipo));
            }
        }
    }

    public void colocarMina(String shipId, int filaVisual, int columna) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            JSONObject target = new JSONObject();
            target.put("x", columna);
            target.put("y", filaLogicaDesdeVisual(filaVisual));
            payload.put("target", target);

            Log.d("DEBUG_ATTACK", "🚀 [PRE-ATAQUE] Emitiendo ataque Mina -> " + payload.toString());
            socket.emit("ship:attack:mine", payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void terminarTurno() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            socket.emit("match:turn_end", payload);
            Log.d("DEBUG_TURNO", "Emit match:turn_end -> " + payload.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rendirse() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            socket.emit("match:surrender", payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void liberarListeners() {
        socketBinder.liberarListeners();
    }
}