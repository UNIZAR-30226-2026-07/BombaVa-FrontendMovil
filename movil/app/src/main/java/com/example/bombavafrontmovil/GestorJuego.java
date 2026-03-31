package com.example.bombavafrontmovil;

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

    boolean esMiTurno = false;

    final GestorJuegoMapper mapper;
    final GestorJuegoSocketBinder socketBinder;

    private boolean invertirPerspectiva = true;

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

    public boolean isInvertirPerspectiva() {
        return invertirPerspectiva;
    }

    public void recalcularPerspectiva(JSONArray myFleet, JSONArray enemyFleet) {
        try {
            if (myFleet == null || enemyFleet == null || myFleet.length() == 0 || enemyFleet.length() == 0) {
                return;
            }

            double mediaMy = mediaY(myFleet);
            double mediaEnemy = mediaY(enemyFleet);

            invertirPerspectiva = mediaMy < mediaEnemy;

            android.util.Log.d(
                    "DEBUG_PERSPECTIVA",
                    "mediaMy=" + mediaMy +
                            " mediaEnemy=" + mediaEnemy +
                            " invertir=" + invertirPerspectiva
            );
        } catch (Exception e) {
            android.util.Log.e("DEBUG_PERSPECTIVA", "Error recalculando perspectiva", e);
        }
    }

    private double mediaY(JSONArray fleet) throws Exception {
        double suma = 0.0;
        for (int i = 0; i < fleet.length(); i++) {
            JSONObject s = fleet.getJSONObject(i);
            suma += s.getInt("y");
        }
        return suma / fleet.length();
    }

    public int filaLogicaDesdeVisual(int filaVisual) {
        return invertirPerspectiva ? (14 - filaVisual) : filaVisual;
    }

    public int filaVisualDesdeLogica(int filaLogica) {
        return invertirPerspectiva ? (14 - filaLogica) : filaLogica;
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

            socket.emit("ship:attack:cannon", payload);
            android.util.Log.d("DEBUG_ATTACK", "Emit ship:attack:cannon -> " + payload.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lanzarTorpedo(String shipId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);
            socket.emit("ship:attack:torpedo", payload);
            android.util.Log.d("DEBUG_ATTACK", "Emit ship:attack:torpedo -> " + payload.toString());
        } catch (Exception e) {
            e.printStackTrace();
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

            socket.emit("ship:attack:mine", payload);
            android.util.Log.d("DEBUG_ATTACK", "Emit ship:attack:mine -> " + payload.toString());
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