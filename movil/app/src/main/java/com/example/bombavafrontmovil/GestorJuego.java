package com.example.bombavafrontmovil;

import android.util.Log;

import com.example.bombavafrontmovil.models.UserShip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private final Map<String, UserShip> diccionarioFlota;
    private final Map<String, UserShip> inventarioOriginal;

    private boolean esMiTurno = false;

    public interface PartidaListener {
        void onSnapshotCompleto(); // solo para startInfo / vision_update
        void onRecursosActualizados(int fuel, int ammo);
        void onPartidaTerminada(String ganadorId, String razon);
        void onErrorJuego(String mensaje);
        void onBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo);
        void onBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo);
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

        configurarListeners();
        unirseAPartida();
    }

    private void unirseAPartida() {
        try {
            Log.d(TAG, "Uniendo a partida... MatchID: " + matchId + " | UserID: " + myUserId);
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

                if (data.has("resources")) {
                    JSONObject res = data.getJSONObject("resources");
                    if (listener != null) {
                        listener.onRecursosActualizados(
                                res.optInt("fuel", -1),
                                res.optInt("ammo", -1)
                        );
                    }
                }
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
                        int oldX = b.x;
                        int oldY = b.y;
                        String orientation = b.orientation;
                        int tipo = b.tipo;

                        int newX = pos.getInt("x");
                        int newY = pos.getInt("y");

                        b.x = newX;
                        b.y = newY;

                        if (listener != null) {
                            listener.onRecursosActualizados(fuelRes, -1);
                            listener.onBarcoMovido(sId, oldX, oldY, newX, newY, orientation, tipo);
                        }
                        return;
                    }
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
                        String oldOrientation = b.orientation;
                        int x = b.x;
                        int y = b.y;
                        int tipo = b.tipo;

                        b.orientation = orientation;

                        if (listener != null) {
                            listener.onRecursosActualizados(fuelRes, -1);
                            listener.onBarcoRotado(sId, x, y, oldOrientation, orientation, tipo);
                        }
                        return;
                    }
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
                        UserShip vinculado = diccionarioFlota.get(s.getString("id"));
                        nuevaFlota.add(construirBarcoDesdeJson(s, true, vinculado));
                    }
                }

                if (data.has("visibleEnemyFleet")) {
                    JSONArray enemyFleet = data.getJSONArray("visibleEnemyFleet");
                    for (int i = 0; i < enemyFleet.length(); i++) {
                        JSONObject s = enemyFleet.getJSONObject(i);
                        nuevaFlota.add(construirBarcoDesdeJson(s, false, null));
                    }
                }

                flota.clear();
                flota.addAll(nuevaFlota);

                if (listener != null) listener.onSnapshotCompleto();
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

            Map<String, UserShip> diccionarioPartida = new HashMap<>();
            HashSet<String> userShipsYaUsados = new HashSet<>();

            if (data.has("playerFleet")) {
                JSONArray fleetArray = data.getJSONArray("playerFleet");
                for (int i = 0; i < fleetArray.length(); i++) {
                    JSONObject s = fleetArray.getJSONObject(i);

                    UserShip matchShip = resolverUserShipParaBarcoPartida(s, userShipsYaUsados);
                    BarcoLogico barco = construirBarcoDesdeJson(s, true, matchShip);

                    flota.add(barco);

                    if (matchShip != null) {
                        diccionarioPartida.put(barco.id, matchShip);
                    }
                }
            }

            if (data.has("enemyFleet")) {
                JSONArray enemyArray = data.getJSONArray("enemyFleet");
                for (int i = 0; i < enemyArray.length(); i++) {
                    JSONObject s = enemyArray.getJSONObject(i);
                    flota.add(construirBarcoDesdeJson(s, false, null));
                }
            }

            diccionarioFlota.clear();
            diccionarioFlota.putAll(diccionarioPartida);

            if (listener != null) listener.onSnapshotCompleto();
        } catch (Exception e) {
            Log.e(TAG, "Error procesando match:startInfo", e);
        }
    }

    private UserShip resolverUserShipParaBarcoPartida(JSONObject s,
                                                      HashSet<String> userShipsYaUsados) throws JSONException {
        if (inventarioOriginal == null || inventarioOriginal.isEmpty()) return null;

        int hp = s.optInt("currentHp", 1);

        for (Map.Entry<String, UserShip> entry : inventarioOriginal.entrySet()) {
            String userShipId = entry.getKey();
            UserShip uShip = entry.getValue();

            if (userShipsYaUsados.contains(userShipId)) continue;
            if (uShip == null || uShip.getShipTemplate() == null) continue;

            if (uShip.getShipTemplate().getBaseMaxHp() == hp) {
                userShipsYaUsados.add(userShipId);
                return uShip;
            }
        }

        for (Map.Entry<String, UserShip> entry : inventarioOriginal.entrySet()) {
            String userShipId = entry.getKey();
            UserShip uShip = entry.getValue();

            if (userShipsYaUsados.contains(userShipId)) continue;
            if (uShip == null) continue;

            userShipsYaUsados.add(userShipId);
            return uShip;
        }

        return null;
    }

    private BarcoLogico construirBarcoDesdeJson(JSONObject s,
                                                boolean esAliado,
                                                UserShip uShipVinculado) throws JSONException {
        String idPartida = s.getString("id");
        int tamanoReal = 1;
        String slugReal = null;

        if (uShipVinculado != null && uShipVinculado.getShipTemplate() != null) {
            tamanoReal = calcularTamanoDesdeTemplate(uShipVinculado);
            slugReal = uShipVinculado.getShipTemplate().getSlug();
        } else {
            UserShip inferido = inferirTemplatePorHp(s.optInt("currentHp", 1));
            if (inferido != null && inferido.getShipTemplate() != null) {
                tamanoReal = calcularTamanoDesdeTemplate(inferido);
                slugReal = inferido.getShipTemplate().getSlug();
            }
        }

        BarcoLogico barco = new BarcoLogico(
                idPartida,
                tamanoReal,
                s.getInt("x"),
                s.getInt("y"),
                s.getString("orientation"),
                esAliado,
                slugReal
        );

        barco.hpActual = s.optInt("currentHp", tamanoReal);

        if (uShipVinculado != null && uShipVinculado.getShipTemplate() != null) {
            barco.hpMax = uShipVinculado.getShipTemplate().getBaseMaxHp();
        } else {
            UserShip inferido = inferirTemplatePorHp(s.optInt("currentHp", 1));
            barco.hpMax = (inferido != null && inferido.getShipTemplate() != null)
                    ? inferido.getShipTemplate().getBaseMaxHp()
                    : barco.hpActual;
        }

        return barco;
    }

    private UserShip inferirTemplatePorHp(int hp) {
        if (inventarioOriginal == null || inventarioOriginal.isEmpty()) return null;

        for (UserShip uShip : inventarioOriginal.values()) {
            if (uShip != null &&
                    uShip.getShipTemplate() != null &&
                    uShip.getShipTemplate().getBaseMaxHp() == hp) {
                return uShip;
            }
        }
        return null;
    }

    private int calcularTamanoDesdeTemplate(UserShip userShip) {
        if (userShip == null || userShip.getShipTemplate() == null) return 1;
        int width = userShip.getShipTemplate().getWidth();
        int height = userShip.getShipTemplate().getHeight();
        return Math.max(width, height);
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
                int filaConvertida = 14 - filaLogica;

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

    public void dispararCannon(String shipId, int filaVisual, int columna) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            JSONObject target = new JSONObject();
            target.put("x", columna);
            target.put("y", 14 - filaVisual);
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

    public void colocarMina(String shipId, int filaVisual, int columna) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);

            JSONObject target = new JSONObject();
            target.put("x", columna);
            target.put("y", 14 - filaVisual);
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