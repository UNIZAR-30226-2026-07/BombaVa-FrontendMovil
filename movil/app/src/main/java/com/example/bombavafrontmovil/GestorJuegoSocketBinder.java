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

                if (data.has("resources") && game.esMiTurno) {
                    JSONObject res = data.getJSONObject("resources");
                    if (game.listener != null) {
                        game.listener.onRecursosActualizados(
                                res.optInt("fuel", -1),
                                res.optInt("ammo", -1)
                        );
                    }
                }

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
                int fuelRes = data.optInt("fuelReserve", -1);

                String userId = data.optString("userId", "");
                boolean esMiAccion = userId.equals(game.myUserId);

                if (game.listener != null && esMiAccion) {
                    game.listener.onRecursosActualizados(fuelRes, -1);
                }

                // IMPORTANTE:
                // No actualizamos posiciones aquí.
                // La fuente de verdad es match:vision_update.
            } catch (Exception e) {
                Log.e(TAG, "Fallo en ship:moved", e);
            }
        });

        game.socket.on("ship:rotated", args -> {
            Log.d(TAG, "¡SERVER RESPONDE! ship:rotated -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];
                int fuelRes = data.optInt("fuelReserve", -1);

                String userId = data.optString("userId", "");
                boolean esMiAccion = userId.equals(game.myUserId);

                if (game.listener != null && esMiAccion) {
                    game.listener.onRecursosActualizados(fuelRes, -1);
                }

                // IMPORTANTE:
                // No actualizamos orientación aquí.
                // La fuente de verdad es match:vision_update.
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
                    if (esMiAtaque) {
                        game.listener.onRecursosActualizados(-1, ammoCurrent);
                    }
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

                List<BarcoLogico> nuevaFlota = new ArrayList<>();

                JSONArray myFleet = data.optJSONArray("myFleet");
                JSONArray enemyFleet = data.optJSONArray("visibleEnemyFleet");

                // Recalcular SOLO perspectiva visual
                if (myFleet != null) {
                    game.recalcularPerspectiva(myFleet, enemyFleet);
                }

                if (myFleet != null) {
                    for (int i = 0; i < myFleet.length(); i++) {
                        JSONObject s = myFleet.getJSONObject(i);
                        nuevaFlota.add(
                                game.mapper.construirBarcoDesdeJson(
                                        s,
                                        true,
                                        game.diccionarioFlota.get(s.getString("id"))
                                )
                        );
                    }
                }

                if (enemyFleet != null) {
                    for (int i = 0; i < enemyFleet.length(); i++) {
                        JSONObject s = enemyFleet.getJSONObject(i);
                        nuevaFlota.add(
                                game.mapper.construirBarcoDesdeJson(
                                        s,
                                        false,
                                        null
                                )
                        );
                    }
                }

                List<BarcoLogico> flotaAnterior = new ArrayList<>(game.flota);

                boolean hayCambios = false;

                if (flotaAnterior.size() != nuevaFlota.size()) {
                    hayCambios = true;
                } else {
                    java.util.Map<String, BarcoLogico> mapaAnterior = new java.util.HashMap<>();
                    for (BarcoLogico b : flotaAnterior) {
                        mapaAnterior.put(b.id, b);
                    }

                    for (BarcoLogico nuevo : nuevaFlota) {
                        BarcoLogico anterior = mapaAnterior.get(nuevo.id);

                        if (anterior == null ||
                                anterior.x != nuevo.x ||
                                anterior.y != nuevo.y ||
                                !java.util.Objects.equals(anterior.orientation, nuevo.orientation) ||
                                anterior.hpActual != nuevo.hpActual ||
                                anterior.hpMax != nuevo.hpMax ||
                                anterior.tipo != nuevo.tipo ||
                                anterior.esAliado != nuevo.esAliado) {
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
                            data.optString("reason", "")
                    );
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
                if (game.listener != null) {
                    game.listener.onErrorJuego(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo en game:error", e);
            }
        });

        game.socket.on("projectile:launched", args -> {
            Log.d("DEBUG_ATTACK", "🌊 [POST-ATAQUE] ¡Torpedo lanzado con éxito! -> " + args[0]);
            try {
                JSONObject data = (JSONObject) args[0];

                String attackerId = data.optString("attackerId", "");
                boolean esMiAtaque = attackerId.equals(game.myUserId);

                if (esMiAtaque) {
                    int nuevaMunicion = data.optInt("ammoCurrent", -1);
                    if (nuevaMunicion != -1 && game.listener != null) {
                        game.listener.onRecursosActualizados(-1, nuevaMunicion);
                    }
                }

                // (Opcional) Si quieres añadir una animación, esto lo ven ambos jugadores:
                // if (game.listener != null) game.listener.onAnimacionTorpedo(...);

            } catch (Exception e) {
                Log.e(TAG, "Fallo al leer projectile:launched", e);
            }
        });
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
    }
}