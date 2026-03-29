package com.example.bombavafrontmovil;

import android.util.Log;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.bombavafrontmovil.models.UserShip;

public class GestorJuego {
    private static final String TAG = "DEBUG_BOMBA";
    private Socket socket;
    private List<BarcoLogico> flota = new ArrayList<>();
    private String matchId, myUserId;
    private PartidaListener listener;
    private boolean esMiTurno = false;

    private Map<String, UserShip> diccionarioFlota;

    public interface PartidaListener {
        void onActualizarTablero();
        void onRecursosActualizados(int fuel, int ammo);
        void onPartidaTerminada(String ganadorId, String razon);
    }

    public GestorJuego(Socket socket, String matchId, String userId, Map<String, UserShip> diccionarioFlota, PartidaListener listener) {
        this.socket = socket;
        this.matchId = matchId;
        this.myUserId = userId;
        this.diccionarioFlota = diccionarioFlota;
        this.listener = listener;
        configurarListeners();
        unirseAPartida();
    }

    private void unirseAPartida() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("userId", myUserId); // Fundamental para que no llegue "null"

            Log.d(TAG, "Uniendo a partida... MatchID: " + matchId + " | UserID: " + myUserId);
            socket.emit("game:join", payload);

        } catch (JSONException e) {
            Log.e(TAG, "Error al crear el payload para game:join", e);
        }
    }

    private void configurarListeners() {
        socket.on("game:joined", (Object... args) -> {
            Log.d(TAG, "Dentro de la sala ok");
        });


        // Carga inicial de posiciones y LOGS
        socket.on("match:startInfo", (Object... args) -> {
            try {
                JSONObject data = (JSONObject) args[0];

                Log.w(TAG, "=================================================");
                Log.w(TAG, "⚓ MATCH START INFO RECIBIDO: " + myUserId);
                Log.w(TAG, data.toString(4));
                Log.w(TAG, "=================================================");

                // Leer el turno inicial para quitar el "CONECTANDO..."
                JSONObject matchInfo = data.optJSONObject("matchInfo");
                if (matchInfo != null) {
                    String currentTurnPlayer = matchInfo.optString("currentTurnPlayer", "");
                    esMiTurno = currentTurnPlayer.equals(myUserId);
                    Log.d(TAG, "Arranque de partida. ¿Es mi turno? " + esMiTurno);
                }

                // Extraemos recursos base
                int ammo = data.optInt("ammo", 0);
                int fuel = data.optInt("fuel", 0);
                if (listener != null) listener.onRecursosActualizados(fuel, ammo);

                // Sacamos la flota
                JSONArray fleetArray = data.getJSONArray("playerFleet");
                flota.clear();

                Map<String, UserShip> diccionarioPartida = new HashMap<>();

                for (int i = 0; i < fleetArray.length(); i++) {
                    JSONObject s = fleetArray.getJSONObject(i);
                    String idPartida = s.getString("id");
                    int hp = s.optInt("currentHp", 0);

                    int tamanoReal = 1;
                    String slugReal = null; // 🔥 PREPARAMOS EL SLUG

                    if (diccionarioFlota != null) {
                        for (UserShip uShip : diccionarioFlota.values()) {
                            // Usamos el HP para emparejar el barco lógico con tu plantilla
                            if (uShip.getShipTemplate() != null && uShip.getShipTemplate().getBaseMaxHp() == hp) {
                                tamanoReal = uShip.getShipTemplate().getTamanoCasillas();
                                slugReal = uShip.getShipTemplate().getSlug(); // 🔥 CORRECCIÓN 2: Extraemos el slug real
                                diccionarioPartida.put(idPartida, uShip);
                                break;
                            }
                        }
                    }

                    // Ahora pasamos el slugReal en lugar de null
                    flota.add(new BarcoLogico(
                            idPartida,
                            tamanoReal,
                            s.getInt("x"),
                            s.getInt("y"),
                            s.getString("orientation"),
                            true,
                            slugReal
                    ));
                }

                diccionarioFlota.clear();
                diccionarioFlota.putAll(diccionarioPartida);

                if (listener != null) listener.onActualizarTablero();

            } catch (Exception e) {
                Log.e(TAG, "Fallo al procesar barcos (match:startInfo)", e);
            }
        });

        // cuando pasamos turno
        socket.on("match:turn_changed", (Object... args) -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String nextPlayerId = data.getString("nextPlayerId");

                esMiTurno = nextPlayerId.equals(myUserId);
                Log.d(TAG, "¿Me toca? " + esMiTurno);

                if (data.has("resources")) {
                    JSONObject res = data.getJSONObject("resources");
                    if (listener != null) listener.onRecursosActualizados(res.getInt("fuel"), res.getInt("ammo"));
                }

                if (listener != null) listener.onActualizarTablero();
            } catch (Exception e) {
                Log.e(TAG, "Fallo en turno", e);
            }
        });

        socket.on("ship:moved", (Object... args) -> {
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
                e.printStackTrace();
            }
        });

        socket.on("ship:attacked", (Object... args) -> {
            try {
                JSONObject data = (JSONObject) args[0];
                int ammoCurrent = data.getInt("ammoCurrent");
                if (listener != null) listener.onRecursosActualizados(-1, ammoCurrent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socket.on("match:finished", (Object... args) -> {
            try {
                JSONObject data = (JSONObject) args[0];
                if (listener != null) listener.onPartidaTerminada(data.getString("winnerId"), data.getString("reason"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socket.on("game:error", (Object... args) -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Log.e(TAG, "Error del server: " + data.getString("message"));
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