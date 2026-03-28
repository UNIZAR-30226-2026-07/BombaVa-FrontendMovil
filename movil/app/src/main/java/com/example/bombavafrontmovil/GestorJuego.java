package com.example.bombavafrontmovil;

import android.util.Log;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class GestorJuego {
    private static final String TAG = "DEBUG_BOMBA";
    private Socket socket;
    private List<BarcoLogico> flota = new ArrayList<>();
    private String matchId, myUserId;
    private PartidaListener listener;
    private boolean esMiTurno = false;

    public interface PartidaListener {
        void onActualizarTablero();
        void onRecursosActualizados(int fuel, int ammo);
        void onPartidaTerminada(String ganadorId, String razon);
    }

    public GestorJuego(Socket socket, String matchId, String userId, PartidaListener listener) {
        this.socket = socket;
        this.matchId = matchId;
        this.myUserId = userId;
        this.listener = listener;
        configurarListeners();
        unirseAPartida();
    }

    private void unirseAPartida() {
        try {
            // Según Operation ID game:join
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            Log.d(TAG, "Emitiendo game:join: " + payload.toString());
            socket.emit("game:join", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear payload game:join", e);
        }
    }

    private void configurarListeners() {
        // game:joined - Confirmación de unión
        socket.on("game:joined", args -> {
            Log.d(TAG, "Servidor confirma unión (game:joined)");
        });

        // match:startInfo - EL MÁS IMPORTANTE (Carga inicial)
        socket.on("match:startInfo", args -> {
            Log.d(TAG, "Recibido match:startInfo");
            try {
                JSONObject data = (JSONObject) args[0];

                // Recursos iniciales
                int ammo = data.optInt("ammo", 0);
                int fuel = data.optInt("fuel", 0);
                if (listener != null) listener.onRecursosActualizados(fuel, ammo);

                // Flota (playerFleet)
                JSONArray fleetArray = data.getJSONArray("playerFleet");
                flota.clear();
                for (int i = 0; i < fleetArray.length(); i++) {
                    JSONObject s = fleetArray.getJSONObject(i);
                    // Creamos el barco lógico (size 1 por defecto ya que no viene en startInfo)
                    flota.add(new BarcoLogico(
                            s.getString("id"),
                            1,
                            s.getInt("x"),
                            s.getInt("y"),
                            s.getString("orientation"),
                            true // Es mi flota
                    ));
                }
                if (listener != null) listener.onActualizarTablero();
            } catch (Exception e) {
                Log.e(TAG, "Error procesando match:startInfo", e);
            }
        });

        // match:turn_changed - Cambio de turno y regeneración
        socket.on("match:turn_changed", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                esMiTurno = data.getString("nextPlayerId").equals(myUserId);

                if (data.has("resources")) {
                    JSONObject res = data.getJSONObject("resources");
                    if (listener != null) listener.onRecursosActualizados(res.getInt("fuel"), res.getInt("ammo"));
                }
                if (listener != null) listener.onActualizarTablero();
            } catch (Exception e) {
                Log.e(TAG, "Error en turn_changed", e);
            }
        });

        // ship:moved - Actualiza posición y combustible tras mover
        socket.on("ship:moved", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String sId = data.getString("shipId");
                JSONObject pos = data.getJSONObject("position");
                int fuelRes = data.getInt("fuelReserve");

                for (BarcoLogico b : flota) {
                    if (b.id.equals(sId)) {
                        b.x = pos.getInt("x");
                        b.y = pos.getInt("y");
                    }
                }
                if (listener != null) {
                    listener.onRecursosActualizados(fuelRes, -1);
                    listener.onActualizarTablero();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en ship:moved", e);
            }
        });

        // ship:attacked - Notifica impacto y gasta munición
        socket.on("ship:attacked", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                int ammoCurrent = data.getInt("ammoCurrent");
                if (listener != null) listener.onRecursosActualizados(-1, ammoCurrent);
                // Aquí podrías añadir lógica para marcar celdas impactadas
            } catch (Exception e) {
                Log.e(TAG, "Error en ship:attacked", e);
            }
        });

        // match:finished - Fin de partida
        socket.on("match:finished", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                if (listener != null) listener.onPartidaTerminada(data.getString("winnerId"), data.getString("reason"));
            } catch (Exception e) {
                Log.e(TAG, "Error en match:finished", e);
            }
        });

        // game:error - Errores de lógica (ej: no es tu turno)
        socket.on("game:error", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Log.e(TAG, "Error de juego: " + data.getString("message"));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public void moverBarco(String shipId, String direction) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject();
            p.put("matchId", matchId);
            p.put("shipId", shipId);
            p.put("direction", direction);
            socket.emit("ship:move", p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void atacarCanon(String shipId, int x, int y) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject();
            p.put("matchId", matchId);
            p.put("shipId", shipId);
            JSONObject t = new JSONObject();
            t.put("x", x); t.put("y", y);
            p.put("target", t);
            socket.emit("ship:attack:cannon", p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void lanzarTorpedo(String shipId) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject();
            p.put("matchId", matchId);
            p.put("shipId", shipId);
            socket.emit("ship:attack:torpedo", p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void ponerMina(String shipId, int x, int y) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject();
            p.put("matchId", matchId);
            p.put("shipId", shipId);
            JSONObject t = new JSONObject();
            t.put("x", x); t.put("y", y);
            p.put("target", t);
            socket.emit("ship:attack:mine", p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void pasarTurno() {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject();
            p.put("matchId", matchId);
            socket.emit("match:turn_end", p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void rendirse() {
        try {
            JSONObject p = new JSONObject();
            p.put("matchId", matchId);
            socket.emit("match:surrender", p);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public BarcoLogico obtenerBarco(String id) {
        for (BarcoLogico b : flota) if (b.id != null && b.id.equals(id)) return b;
        return null;
    }
    public boolean isEsMiTurno() { return esMiTurno; }
    public List<BarcoLogico> getFlota() { return flota; }
    public void desconectar() {
        if (socket != null) {
            socket.off("match:startInfo");
            socket.off("match:turn_changed");
            socket.off("ship:moved");
            socket.off("match:finished");
        }
    }
}