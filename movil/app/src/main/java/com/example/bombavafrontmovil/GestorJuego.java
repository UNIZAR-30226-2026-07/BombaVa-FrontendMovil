package com.example.bombavafrontmovil;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GestorJuego {
    private Socket socket;
    private List<BarcoLogico> flota = new ArrayList<>();
    private String matchId;
    private String myUserId;
    private Runnable onUpdateListener;
    private boolean esMiTurno = true; // Asumimos true al principio, el server lo corregirá

    public GestorJuego(String token, String matchId, String userId, Runnable onUpdateListener) {
        this.matchId = matchId;
        this.myUserId = userId;
        this.onUpdateListener = onUpdateListener;
        conectarSocket(token);
    }

    private void conectarSocket(String token) {
        try {
            IO.Options opts = new IO.Options();
            opts.auth = java.util.Collections.singletonMap("token", token);
            opts.transports = new String[]{"websocket"};
            socket = IO.socket("http://10.0.2.2:3000", opts);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("BombaVa", " CONECTADO AL SERVIDOR OFICIAL");
                socket.emit("game:join", matchId);
                socket.emit("game:get_status", matchId); // Mantenemos esto por si acaso
            });

            // 1. RECIBIR ESTADO
            socket.on("game:status", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    JSONArray players = data.getJSONArray("players");
                    flota.clear();

                    for (int i = 0; i < players.length(); i++) {
                        JSONObject p = players.getJSONObject(i);
                        String pUserId = p.getString("userId");
                        boolean esMio = pUserId.equals(myUserId);

                        JSONArray ships = p.getJSONArray("ships");
                        for (int j = 0; j < ships.length(); j++) {
                            JSONObject s = ships.getJSONObject(j);
                            JSONObject pos = s.getJSONObject("position");

                            flota.add(new BarcoLogico(
                                    s.getString("id"), s.getInt("size"),
                                    pos.getInt("x"), pos.getInt("y"),
                                    s.getString("orientation"), esMio
                            ));
                        }
                    }
                    if (onUpdateListener != null) onUpdateListener.run();
                } catch (Exception e) { Log.e("BombaVa", "Error estado: " + e.getMessage()); }
            });

            // 2. MOVIMIENTO
            socket.on("ship:moved", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String shipId = data.getString("shipId");
                    JSONObject pos = data.getJSONObject("position");
                    for (BarcoLogico b : flota) {
                        if (b.id != null && b.id.equals(shipId)) {
                            b.x = pos.getInt("x");
                            b.y = pos.getInt("y");
                            break;
                        }
                    }
                    if (onUpdateListener != null) onUpdateListener.run();
                } catch (Exception e) { e.printStackTrace(); }
            });

            // 3. CAMBIO DE TURNO
            socket.on("match:turn_changed", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String nextPlayerId = data.getString("nextPlayerId");
                    esMiTurno = nextPlayerId.equals(myUserId);
                    Log.d("BombaVa", "🔄 Turno cambiado. ¿Es el mío?: " + esMiTurno);
                    if (onUpdateListener != null) onUpdateListener.run();
                } catch (Exception e) { e.printStackTrace(); }
            });

            // 4. RESULTADO DE ATAQUE
            socket.on("ship:attacked", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    boolean hit = data.getBoolean("hit");
                    Log.d("BombaVa", hit ? "💥 ¡IMPACTO CONFIRMADO!" : "💦 ¡AGUA!");
                    // Aquí podrías actualizar la vida de la casilla objetivo
                } catch (Exception e) { e.printStackTrace(); }
            });

            socket.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    // --- ACCIONES AL SERVIDOR ---

    public void moverBarco(String shipId, String direction) {
        if (!esMiTurno) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId);
            payload.put("direction", direction);
            socket.emit("ship:move", payload);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // Adaptado al endpoint oficial: ship:attack:cannon
    public void atacarCanon(String shipId, int targetX, int targetY) {
        if (!esMiTurno) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            payload.put("shipId", shipId); // La doc oficial pide shipId

            JSONObject target = new JSONObject();
            target.put("x", targetX);
            target.put("y", targetY);
            payload.put("target", target);

            socket.emit("ship:attack:cannon", payload);
            Log.d("BombaVa", "🚀 Disparando cañón a " + targetX + "," + targetY);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // Adaptado al endpoint oficial: match:turn_end
    public void pasarTurno() {
        if (!esMiTurno) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            socket.emit("match:turn_end", payload);
            esMiTurno = false; // Bloqueo local instantáneo
            if (onUpdateListener != null) onUpdateListener.run();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public boolean isEsMiTurno() { return esMiTurno; }
    public List<BarcoLogico> getFlota() { return flota; }
    public void desconectar() { if (socket != null) socket.disconnect(); }
}