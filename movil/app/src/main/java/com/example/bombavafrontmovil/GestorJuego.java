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

    // Solo controla cómo se PINTA el tablero
    //private boolean invertirPerspectiva = true;

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
        // Buscamos nuestro primer barco aliado para saber dónde nos puso el servidor
        for (BarcoLogico b : flota) {
            if (b.esAliado) {
                // Si la Y lógica de mi barco está en la mitad superior (Norte del server),
                // necesito invertir mi pantalla para jugar desde abajo (Sur visual).
                return b.y < 7;
            }
        }
        return false; // Por defecto no invertimos
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
            android.util.Log.d("DEBUG_JOIN", "Emit game:join -> " + matchId);
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
            android.util.Log.d("DEBUG_MOVE", "Emit ship:move -> " + payload.toString());
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
            android.util.Log.d("DEBUG_ROTATE", "Emit ship:rotate -> " + payload.toString());
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

    // 🌊 REGISTRA EL TORPEDO (Ya no usamos la memoria, usamos los datos reales del server)
    // Añadimos 'String tipo' al final
    public void registrarTorpedoDesdeServer(String id, int x, int y, int vectorX, int vectorY, int lifeDistance, boolean esAliado, String tipo) {
        TorpedoLogico nuevoProyectil = new TorpedoLogico(id, x, y, vectorX, vectorY, lifeDistance, esAliado, tipo);
        torpedosActivos.add(nuevoProyectil); // Guardamos la mina en la misma lista

        if (listener != null) {
            listener.onSnapshotCompleto();
        }
    }

    // 💥 PROCESA EL IMPACTO, BAJA LA VIDA Y HUNDE BARCOS
    public void procesarImpactoTorpedo(String shipId, String proyectilId, int newHp) {
        // 1. Borramos el torpedo del agua
        TorpedoLogico torpedoRoto = null;
        for (TorpedoLogico t : torpedosActivos) {
            if (t.id.equals(proyectilId)) {
                torpedoRoto = t;
                break;
            }
        }
        if (torpedoRoto != null) torpedosActivos.remove(torpedoRoto);

        // 2. Buscamos el barco y le restamos la vida
        BarcoLogico barcoHundido = null;
        for (BarcoLogico b : flota) {
            if (b.id.equals(shipId)) {
                b.hpActual = newHp;
                Log.d("DEBUG_TORPEDO", "💥 ¡Impacto en barco " + shipId + "! Vida restante: " + newHp);

                // SI LA VIDA LLEGA A 0, LO MARCAMOS PARA EL FONDO DEL MAR
                if (b.hpActual <= 0) {
                    barcoHundido = b;
                    Log.d("DEBUG_TORPEDO", "🏴‍☠️ ¡Barco " + shipId + " HUNDIDO! Desaparece del mapa.");
                }
                break;
            }
        }

        // Si se ha hundido, lo eliminamos de la flota para que deje de pintarse
        if (barcoHundido != null) {
            flota.remove(barcoHundido);
        }

        // 3. Forzamos al tablero a pintarse
        if (listener != null) {
            listener.onSnapshotCompleto();
        }
    }

    public void avanzarTorpedos() {
        Log.d("DEBUG_TORPEDO", "El frontend ya no mueve torpedos. Esperando órdenes del servidor...");
    }

    // CTUALIZAMOS EL TORPEDO SEGÚN EL SERVIDOR
    public void actualizarTorpedo(String id, String status, int newX, int newY, int newLife) {
        for (TorpedoLogico t : torpedosActivos) {
            if (t.id.equals(id)) {
                if (newX != -1) t.x = newX;
                if (newY != -1) t.y = newY;
                if (newLife != -1) t.lifeDistance = newLife;

                Log.d("DEBUG_TORPEDO_VISUAL", "📍 Servidor movió el torpedo " + id.substring(0,4) + " a (" + t.x + "," + t.y + ")");

                // Si el servidor dice que va a morir, podríamos hacer algo, pero por ahora
                // simplemente esperamos al evento projectile:hit o al vision_update para borrarlo
                break;
            }
        }

        // Forzamos repintado para que se mueva en la pantalla
        if (listener != null) {
            listener.onSnapshotCompleto();
        }
    }

    // SINCRONIZACIÓN TOTAL DE VISIÓN
    public void sincronizarProyectilesVision(JSONArray proyPropios, JSONArray proyEnemigos) {
        torpedosActivos.clear();
        try {
            if (proyPropios != null) {
                for (int i = 0; i < proyPropios.length(); i++) {
                    JSONObject p = proyPropios.getJSONObject(i);
                    registrarTorpedoDesdeServer(
                            p.getString("id"), p.getInt("x"), p.getInt("y"),
                            p.optInt("vectorX", 0), p.optInt("vectorY", 0),
                            p.optInt("lifeDistance", 0), true,
                            p.optString("type", "TORPEDO")
                    );
                }
            }
            if (proyEnemigos != null) {
                for (int i = 0; i < proyEnemigos.length(); i++) {
                    JSONObject p = proyEnemigos.getJSONObject(i);
                    registrarTorpedoDesdeServer(
                            p.getString("id"), p.getInt("x"), p.getInt("y"),
                            p.optInt("vectorX", 0), p.optInt("vectorY", 0),
                            p.optInt("lifeDistance", 0), false,
                            p.optString("type", "TORPEDO")
                    );
                }
            }
            Log.d("DEBUG_TORPEDO", "👁️ Visión actualizada: " + torpedosActivos.size() + " proyectiles.");
        } catch (Exception e) {
            Log.e("DEBUG_TORPEDO", "Error procesando proyectiles de vision_update", e);
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
            android.util.Log.d("DEBUG_TURNO", "Emit match:turn_end -> " + payload.toString());
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