package com.example.bombavafrontmovil;

import android.util.Log;

import com.example.bombavafrontmovil.models.UserShip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;

public class GestorJuego {
    private static final String TAG = "DEBUG_BOMBA";

    private final Socket socket;
    private final List<BarcoLogico> flota = new ArrayList<>();
    private final String matchId;
    private final String myUserId;
    private final PartidaListener listener;
    private boolean esMiTurno = false;

    private final Map<String, UserShip> diccionarioFlota;

    public interface PartidaListener {
        void onActualizarTablero();
        void onRecursosActualizados(int fuel, int ammo);
        void onPartidaTerminada(String ganadorId, String razon);
        void onErrorJuego(String mensaje);
    }

    public GestorJuego(Socket socket, String matchId, String userId,
                       Map<String, UserShip> diccionarioFlota,
                       PartidaListener listener) {
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
            Log.d(TAG, "Uniendo a partida... MatchID: " + matchId + " | UserID: " + myUserId);
            // IMPORTANTE: el backend real espera el matchId directo, no un objeto JSON
            socket.emit("game:join", matchId);
        } catch (Exception e) {
            Log.e(TAG, "Error al emitir game:join", e);
        }
    }

    private void configurarListeners() {
        socket.on("game:joined", args -> Log.d(TAG, "Dentro de la sala ok"));

        socket.on("match:startInfo", this::procesarStartInfo);

        socket.on("match:turn_changed", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String nextPlayerId = data.getString("nextPlayerId");

                esMiTurno = nextPlayerId.equals(myUserId);
                Log.d(TAG, "¿Me toca? " + esMiTurno);

                if (data.has("resources")) {
                    JSONObject res = data.getJSONObject("resources");
                    if (listener != null) {
                        listener.onRecursosActualizados(
                                res.optInt("fuel", -1),
                                res.optInt("ammo", -1)
                        );
                    }
                }

                if (listener != null) listener.onActualizarTablero();
            } catch (Exception e) {
                Log.e(TAG, "Fallo en match:turn_changed", e);
            }
        });

        socket.on("ship:moved", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String sId = data.getString("shipId");
                JSONObject pos = data.getJSONObject("position");
                int fuelRes = data.optInt("fuelReserve", -1);

                for (BarcoLogico b : flota) {
                    if (b.id.equals(sId)) {
                        b.x = pos.getInt("x");
                        b.y = pos.getInt("y");
                        break;
                    }
                }

                if (listener != null) {
                    listener.onRecursosActualizados(fuelRes, -1);
                    listener.onActualizarTablero();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:moved", e);
            }
        });

        socket.on("ship:rotated", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String sId = data.getString("shipId");
                String orientation = data.getString("orientation");
                int fuelRes = data.optInt("fuelReserve", -1);

                for (BarcoLogico b : flota) {
                    if (b.id.equals(sId)) {
                        b.orientation = orientation;
                        break;
                    }
                }

                if (listener != null) {
                    listener.onRecursosActualizados(fuelRes, -1);
                    listener.onActualizarTablero();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:rotated", e);
            }
        });

        socket.on("ship:attacked", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                int ammoCurrent = data.optInt("ammoCurrent", -1);

                if (listener != null) {
                    listener.onRecursosActualizados(-1, ammoCurrent);
                    listener.onActualizarTablero();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:attacked", e);
            }
        });

        socket.on("match:vision_update", args -> {
            try {
                JSONObject data = (JSONObject) args[0];

                List<BarcoLogico> nuevaFlota = new ArrayList<>();

                if (data.has("myFleet")) {
                    JSONArray myFleet = data.getJSONArray("myFleet");
                    for (int i = 0; i < myFleet.length(); i++) {
                        JSONObject s = myFleet.getJSONObject(i);
                        nuevaFlota.add(construirBarcoDesdeJson(s, true));
                    }
                }

                if (data.has("visibleEnemyFleet")) {
                    JSONArray enemyFleet = data.getJSONArray("visibleEnemyFleet");
                    for (int i = 0; i < enemyFleet.length(); i++) {
                        JSONObject s = enemyFleet.getJSONObject(i);
                        nuevaFlota.add(construirBarcoDesdeJson(s, false));
                    }
                }

                flota.clear();
                flota.addAll(nuevaFlota);

                if (listener != null) listener.onActualizarTablero();
            } catch (Exception e) {
                Log.e(TAG, "Fallo en match:vision_update", e);
            }
        });

        socket.on("match:finished", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                if (listener != null) {
                    listener.onPartidaTerminada(
                            data.optString("winnerId", ""),
                            data.optString("reason", "")
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en match:finished", e);
            }
        });

        socket.on("game:error", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String msg = data.optString("message", "Error de juego");
                Log.e(TAG, "Error del server: " + msg);
                if (listener != null) listener.onErrorJuego(msg);
            } catch (Exception e) {
                Log.e(TAG, "Fallo en game:error", e);
            }
        });
    }

    public void procesarStartInfo(Object[] args) {
        try {
            JSONObject data = (JSONObject) args[0];

            JSONObject matchInfo = data.optJSONObject("matchInfo");
            if (matchInfo != null) {
                String currentTurnPlayer = matchInfo.optString("currentTurnPlayer", "");
                String yourId = matchInfo.optString("yourId", myUserId);
                esMiTurno = currentTurnPlayer.equals(yourId);
                Log.d(TAG, "Turno inicial: " + esMiTurno);
            }

            int ammo = data.optInt("ammo", 0);
            int fuel = data.optInt("fuel", 0);
            if (listener != null) listener.onRecursosActualizados(fuel, ammo);

            flota.clear();

            if (data.has("playerFleet")) {
                JSONArray fleetArray = data.getJSONArray("playerFleet");
                for (int i = 0; i < fleetArray.length(); i++) {
                    JSONObject s = fleetArray.getJSONObject(i);
                    flota.add(construirBarcoDesdeJson(s, true));
                }
            }

            if (data.has("enemyFleet")) {
                JSONArray enemyArray = data.getJSONArray("enemyFleet");
                for (int i = 0; i < enemyArray.length(); i++) {
                    JSONObject s = enemyArray.getJSONObject(i);
                    flota.add(construirBarcoDesdeJson(s, false));
                }
            }

            if (listener != null) listener.onActualizarTablero();
        } catch (Exception e) {
            Log.e(TAG, "Error procesando match:startInfo", e);
        }
    }

    private BarcoLogico construirBarcoDesdeJson(JSONObject s, boolean esAliado) throws JSONException {
        String idPartida = s.getString("id");
        int hp = s.optInt("currentHp", 1);

        int tamanoReal = 1;
        String slugReal = null;

        if (diccionarioFlota != null) {
            for (UserShip uShip : diccionarioFlota.values()) {
                if (uShip.getShipTemplate() != null &&
                        uShip.getShipTemplate().getBaseMaxHp() == hp) {
                    tamanoReal = uShip.getShipTemplate().getTamanoCasillas();
                    slugReal = uShip.getShipTemplate().getSlug();
                    break;
                }
            }
        }

        return new BarcoLogico(
                idPartida,
                tamanoReal,
                s.getInt("x"),
                s.getInt("y"),
                s.getString("orientation"),
                esAliado,
                slugReal
        );
    }

    public boolean isEsMiTurno() {
        return esMiTurno;
    }

    public List<BarcoLogico> getFlota() {
        return flota;
    }

    public BarcoLogico obtenerBarcoEn(int fila, int columna) {
        for (BarcoLogico b : flota) {
            for (int[] c : b.getCeldas()) {
                if (c[0] == fila && c[1] == columna) return b;
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
        } catch (Exception e) {
            Log.e(TAG, "Error moviendo barco", e);
        }
    }

    public void rotarBarco(String shipId, int degrees) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);
            payload.put("degrees", degrees);
            socket.emit("ship:rotate", payload);
        } catch (Exception e) {
            Log.e(TAG, "Error rotando barco", e);
        }
    }

    public void dispararCannon(String shipId, int fila, int columna) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            JSONObject target = new JSONObject();
            target.put("x", columna);
            target.put("y", fila);
            payload.put("target", target);

            socket.emit("ship:attack:cannon", payload);
        } catch (Exception e) {
            Log.e(TAG, "Error disparando cañón", e);
        }
    }

    public void lanzarTorpedo(String shipId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);
            socket.emit("ship:attack:torpedo", payload);
        } catch (Exception e) {
            Log.e(TAG, "Error lanzando torpedo", e);
        }
    }

    public void colocarMina(String shipId, int fila, int columna) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            JSONObject target = new JSONObject();
            target.put("x", columna);
            target.put("y", fila);
            payload.put("target", target);

            socket.emit("ship:attack:mine", payload);
        } catch (Exception e) {
            Log.e(TAG, "Error colocando mina", e);
        }
    }

    public void terminarTurno() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            socket.emit("match:turn_end", payload);
        } catch (Exception e) {
            Log.e(TAG, "Error terminando turno", e);
        }
    }

    public void rendirse() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            socket.emit("match:surrender", payload);
        } catch (Exception e) {
            Log.e(TAG, "Error al rendirse", e);
        }
    }

    public void liberarListeners() {
        socket.off("game:joined");
        socket.off("match:startInfo");
        socket.off("match:turn_changed");
        socket.off("ship:moved");
        socket.off("ship:rotated");
        socket.off("ship:attacked");
        socket.off("match:vision_update");
        socket.off("match:finished");
        socket.off("game:error");
    }
}