package com.example.bombavafrontmovil;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GestorJuegoSocketBinder {

    private static final String TAG = "DEBUG_BOMBA";
    private final GestorJuego game;

    public GestorJuegoSocketBinder(GestorJuego game) {
        this.game = game;
    }

    public void configurarListeners() {
        liberarListeners();

        game.socket.on("game:joined", args -> Log.d(TAG, "¡SERVER CONFIRMA! game:joined recibido."));

        game.socket.on("match:startInfo", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                android.util.Log.d("DEBUG_ORIENTACION", "📥 [SOCKET] match:startInfo recibido COMPLETO: " + data.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            game.procesarStartInfo(args);
        });

        game.socket.on("match:turn_changed", args -> {
            Log.d(TAG, "¡SERVER RESPONDE! match:turn_changed -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                String nextPlayerId = data.getString("nextPlayerId");
                game.esMiTurno = nextPlayerId.equals(game.myUserId);

                if (game.listener != null) {
                    game.listener.onSnapshotCompleto();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en match:turn_changed", e);
            }
        });

        game.socket.on("ship:moved", args -> {
            Log.d(TAG, "¡SERVER RESPONDE! ship:moved -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                String sId = data.getString("shipId");
                JSONObject pos = data.getJSONObject("position");
                int fuelRes = data.optInt("fuelReserve", -1);

                String userId = data.optString("userId", "");
                boolean esMiAccion = userId.equals(game.myUserId);

                for (BarcoLogico b : game.flota) {
                    if (b.id.equals(sId)) {
                        int oldX = b.x;
                        int oldY = b.y;
                        String orientation = b.orientation;
                        int tipo = b.tipo;

                        int newX = pos.getInt("x");
                        int newY = pos.getInt("y");

                        b.x = newX;
                        b.y = newY;

                        if (game.listener != null) {
                            if (esMiAccion) game.listener.onRecursosActualizados(fuelRes, -1);
                            game.listener.onBarcoMovido(sId, oldX, oldY, newX, newY, orientation, tipo);
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:moved", e);
            }
        });

        game.socket.on("ship:rotated", args -> {
            Log.d(TAG, "¡SERVER RESPONDE! ship:rotated -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                String sId = data.getString("shipId");
                String orientation = data.getString("orientation");
                int fuelRes = data.optInt("fuelReserve", -1);

                String userId = data.optString("userId", "");
                boolean esMiAccion = userId.equals(game.myUserId);

                for (BarcoLogico b : game.flota) {
                    if (b.id.equals(sId)) {
                        String oldOrientation = b.orientation;
                        int x = b.x;
                        int y = b.y;
                        int tipo = b.tipo;

                        b.orientation = orientation;

                        if (game.listener != null) {
                            if (esMiAccion) game.listener.onRecursosActualizados(fuelRes, -1);
                            game.listener.onBarcoRotado(sId, x, y, oldOrientation, orientation, tipo);
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:rotated", e);
            }
        });

        game.socket.on("ship:attacked", args -> {
            Log.d("DEBUG_ATTACK", "💥 [POST-ATAQUE] Respuesta del server -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                int ammoCurrent = data.optInt("ammoCurrent", -1);
                String attackerId = data.optString("attackerId", "");
                boolean esMiAtaque = attackerId.equals(game.myUserId);

                if (game.listener != null) {
                    if (esMiAtaque) game.listener.onRecursosActualizados(-1, ammoCurrent);
                    game.listener.onSnapshotCompleto();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:attacked", e);
            }
        });

        game.socket.on("match:vision_update", args -> {
            Log.d(TAG, "¡SERVER RESPONDE! match:vision_update -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                Log.w("DEBUG_TORPEDO", "📩 [UPDATED] Recibido del server: " + data.toString());

                List<BarcoLogico> nuevaFlota = new ArrayList<>();
                JSONArray myFleet = data.optJSONArray("myFleet");
                JSONArray enemyFleet = data.optJSONArray("visibleEnemyFleet");

                if (myFleet != null) {
                    for (int i = 0; i < myFleet.length(); i++) {
                        JSONObject s = myFleet.getJSONObject(i);
                        nuevaFlota.add(game.mapper.construirBarcoDesdeJson(
                                s, true, game.diccionarioFlota.get(s.getString("id"))));
                    }
                }
                if (enemyFleet != null) {
                    for (int i = 0; i < enemyFleet.length(); i++) {
                        JSONObject s = enemyFleet.getJSONObject(i);
                        nuevaFlota.add(game.mapper.construirBarcoDesdeJson(s, false, null));
                    }
                }

                List<BarcoLogico> flotaAnterior = new ArrayList<>(game.flota);
                boolean hayCambios = flotaAnterior.size() != nuevaFlota.size();

                if (!hayCambios) {
                    java.util.Map<String, BarcoLogico> mapaAnterior = new java.util.HashMap<>();
                    for (BarcoLogico b : flotaAnterior) mapaAnterior.put(b.id, b);
                    for (BarcoLogico nuevo : nuevaFlota) {
                        BarcoLogico anterior = mapaAnterior.get(nuevo.id);
                        if (anterior == null
                                || anterior.x != nuevo.x || anterior.y != nuevo.y
                                || !java.util.Objects.equals(anterior.orientation, nuevo.orientation)
                                || anterior.hpActual != nuevo.hpActual
                                || anterior.hpMax != nuevo.hpMax
                                || anterior.tipo != nuevo.tipo
                                || anterior.esAliado != nuevo.esAliado) {
                            hayCambios = true;
                            break;
                        }
                    }
                }

                game.flota.clear();
                game.flota.addAll(nuevaFlota);

                if (hayCambios && game.listener != null) {
                    game.listener.onVisionUpdateParcial(flotaAnterior, nuevaFlota);
                }

                // El vision_update es la fuente de verdad: sobreescribe cualquier
                // movimiento local optimista que hayamos aplicado en avanzarTorpedosLocalmente().
                JSONArray proyPropios = data.optJSONArray("proyPropios");
                JSONArray proyEnemigos = data.optJSONArray("proyEnemigos");
                game.sincronizarProyectilesVision(proyPropios, proyEnemigos);

                if (game.listener != null) {
                    game.listener.onSnapshotCompleto();
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en match:vision_update", e);
            }
        });

        game.socket.on("match:finished", args -> {
            Log.d(TAG, "¡SERVER RESPONDE! match:finished -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                if (game.listener != null) {
                    game.listener.onPartidaTerminada(
                            data.optString("winnerId", ""),
                            data.optString("reason", ""));
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en match:finished", e);
            }
        });

        game.socket.on("game:error", args -> {
            Log.e("DEBUG_ATTACK", "❌ [ERROR SERVER] -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                String msg = data.optString("message", "Error de juego");
                if (game.listener != null) game.listener.onErrorJuego(msg);
            } catch (Exception e) {
                Log.e(TAG, "Fallo en game:error", e);
            }
        });

        // ─── EVENTOS DE PROYECTILES ───────────────────────────────────────────────

        // Ponemos escuchas para TODAS las posibles variaciones que el backend pueda estar usando
        game.socket.on("projectile:updated", args -> {
            Log.d("DEBUG_BOMBA", "📡 [SOCKET] ¡RECIBIDO projectile:updated (CON D)!");
            procesarProyectilUpdate(args);
        });

        game.socket.on("projectile:update", args -> {
            Log.d("DEBUG_BOMBA", "📡 [SOCKET] ¡RECIBIDO projectile:update (SIN D)!");
            procesarProyectilUpdate(args);
        });

        game.socket.on("projectile:launched", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String projectileId = data.getString("id");
                String type = data.getString("type");
                int x = data.getInt("x");
                int y = data.getInt("y");
                int vectorX = data.optInt("vectorX", 0);
                int vectorY = data.optInt("vectorY", 0);
                int lifeDistance = data.getInt("lifeDistance");
                int ammoCurrent = data.optInt("ammoCurrent", -1);
                String ownerId = data.getString("ownerId");

                boolean esMiAtaque = ownerId.equals(game.myUserId);
                if (esMiAtaque && ammoCurrent != -1 && game.listener != null) {
                    game.listener.onRecursosActualizados(-1, ammoCurrent);
                }
                game.registrarTorpedoDesdeServer(projectileId, x, y, vectorX, vectorY, lifeDistance, esMiAtaque, type);
            } catch (Exception e) {
                Log.e(TAG, "Fallo al procesar projectile:launched", e);
            }
        });

        game.socket.on("projectile:hit", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Log.d("DEBUG_TORPEDO", "💥 [IMPACTO] -> " + data.toString());
                String shipId = data.getString("shipId");
                String proyectilId = data.getString("proyectilColisionado");
                int newHp = data.getInt("newHp");
                game.procesarImpactoTorpedo(shipId, proyectilId, newHp);
            } catch (Exception e) {
                Log.e(TAG, "Fallo al procesar projectile:hit", e);
            }
        });
    }

    /**
     * Procesa el evento de actualización de posición de un proyectil en vuelo.
     * El backend emite esto durante la resolución de turno para cada torpedo activo.
     * Las coordenadas x,y que llegan aquí ya están traducidas a la perspectiva del jugador.
     */
    private void procesarProyectilUpdate(Object[] args) {
        try {
            JSONObject data = (JSONObject) args[0];
            String id = data.getString("projectile");
            String status = data.getString("status");

            if ("ENDOFLIFE".equals(status)) {
                TorpedoLogico aEliminar = null;
                for (TorpedoLogico t : game.torpedosActivos) {
                    if (t.id.equals(id)) { aEliminar = t; break; }
                }
                if (aEliminar != null) {
                    game.torpedosActivos.remove(aEliminar);
                    Log.d("DEBUG_TORPEDO", "🕳️ Torpedo "
                            + id.substring(0, Math.min(4, id.length())) + " eliminado (ENDOFLIFE)");
                }
            } else {
                int x = data.has("x") && !data.isNull("x") ? data.getInt("x") : -1;
                int y = data.has("y") && !data.isNull("y") ? data.getInt("y") : -1;
                int life = data.optInt("lifeDistance", -1);
                game.actualizarTorpedo(id, status, x, y, life);
            }

            if (game.listener != null) game.listener.onSnapshotCompleto();
        } catch (Exception e) {
            Log.e(TAG, "Fallo al procesar projectile:update", e);
        }
    }

    public void liberarListeners() {
        game.socket.off("game:joined");
        game.socket.off("match:startInfo");
        game.socket.off("match:turn_changed");
        game.socket.off("ship:moved");
        game.socket.off("ship:rotated");
        game.socket.off("ship:attacked");
        game.socket.off("match:vision_update");
        game.socket.off("match:finished");
        game.socket.off("game:error");
        game.socket.off("projectile:update");
        game.socket.off("projectile:updated");
        game.socket.off("projectile:launched");
        game.socket.off("projectile:hit");
    }
}