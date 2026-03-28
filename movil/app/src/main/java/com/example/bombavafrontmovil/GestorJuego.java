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
    private PartidaListener listener;
    private boolean esMiTurno = true;

    //  Interfaz de comunicación avanzada con la pantalla
    public interface PartidaListener {
        void onActualizarTablero();
        void onRecursosActualizados(int fuel, int ammo);
        void onPartidaTerminada(String ganadorId, String razon);
    }

    public GestorJuego(String token, String matchId, String userId, PartidaListener listener) {
        this.matchId = matchId;
        this.myUserId = userId;
        this.listener = listener;
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
                socket.emit("game:get_status", matchId);
            });

            // 1. ESTADO INICIAL
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
                    if (listener != null) listener.onActualizarTablero();
                } catch (Exception e) { Log.e("BombaVa", "Error estado: " + e.getMessage()); }
            });

            // 2. MOVIMIENTO (Y coste de combustible )
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

                    // Si el servidor nos devuelve el fuel restante, lo actualizamos
                    if(data.has("fuelReserve") && listener != null) {
                        // Asumimos municion intacta al mover
                        listener.onRecursosActualizados(data.getInt("fuelReserve"), -1);
                    }
                    if (listener != null) listener.onActualizarTablero();
                } catch (Exception e) { e.printStackTrace(); }
            });

            // 3. CAMBIO DE TURNO Y RECURSOS
            socket.on("match:turn_changed", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    esMiTurno = data.getString("nextPlayerId").equals(myUserId);

                    // Leer los nuevos recursos (Combustible y Munición)
                    if (data.has("resources")) {
                        JSONObject res = data.getJSONObject("resources");
                        int fuel = res.getInt("fuel");
                        int ammo = res.getInt("ammo");
                        if (listener != null) listener.onRecursosActualizados(fuel, ammo);
                    }
                    if (listener != null) listener.onActualizarTablero();
                } catch (Exception e) { e.printStackTrace(); }
            });

            // 4. RESULTADO DE ATAQUE
            socket.on("ship:attacked", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    boolean hit = data.getBoolean("hit");
                    Log.d("BombaVa", hit ? "💥 ¡IMPACTO!" : "💦 ¡AGUA!");

                    // Actualizar munición si viene en el ataque
                    if (data.has("ammoCurrent") && listener != null) {
                        listener.onRecursosActualizados(-1, data.getInt("ammoCurrent"));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });

            // 5. FIN DE LA PARTIDA
            socket.on("match:finished", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String winnerId = data.getString("winnerId");
                    String reason = data.getString("reason");
                    if (listener != null) listener.onPartidaTerminada(winnerId, reason);
                } catch (Exception e) { e.printStackTrace(); }
            });

            socket.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    // --- ACCIONES DE JUEGO ---

    public void moverBarco(String shipId, String direction) {
        if (!esMiTurno) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId); payload.put("shipId", shipId); payload.put("direction", direction);
            socket.emit("ship:move", payload);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void atacarCanon(String shipId, int targetX, int targetY) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject(); p.put("matchId", matchId); p.put("shipId", shipId);
            JSONObject target = new JSONObject(); target.put("x", targetX); target.put("y", targetY);
            p.put("target", target);
            socket.emit("ship:attack:cannon", p);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    //  Lanzar Torpedo (Va recto, no necesita X e Y)
    public void lanzarTorpedo(String shipId) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject(); p.put("matchId", matchId); p.put("shipId", shipId);
            socket.emit("ship:attack:torpedo", p);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    //  Poner Mina (Necesita X e Y adyacente)
    public void ponerMina(String shipId, int targetX, int targetY) {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject(); p.put("matchId", matchId); p.put("shipId", shipId);
            JSONObject target = new JSONObject(); target.put("x", targetX); target.put("y", targetY);
            p.put("target", target);
            socket.emit("ship:attack:mine", p);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void pasarTurno() {
        if (!esMiTurno) return;
        try {
            JSONObject p = new JSONObject(); p.put("matchId", matchId);
            socket.emit("match:turn_end", p);
            esMiTurno = false;
            if (listener != null) listener.onActualizarTablero();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // Rendirse
    public void rendirse() {
        try {
            JSONObject p = new JSONObject(); p.put("matchId", matchId);
            socket.emit("match:surrender", p);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public BarcoLogico obtenerBarco(String idBuscado) {
        for (BarcoLogico b : flota) {
            if (b.id != null && b.id.equals(idBuscado)) return b;
        }
        return null;
    }

    public boolean isEsMiTurno() { return esMiTurno; }
    public List<BarcoLogico> getFlota() { return flota; }
    public void desconectar() { if (socket != null) socket.disconnect(); }
}