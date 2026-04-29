package com.example.bombavafrontmovil;

import android.util.Log;

import com.example.bombavafrontmovil.models.UserShip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GestorJuegoMapper {

    private static final String TAG = "DEBUG_BOMBA";
    private final GestorJuego game;

    public GestorJuegoMapper(GestorJuego game) {
        this.game = game;
    }

    public void procesarStartInfo(Object[] args) {
        try {
            JSONObject data = (JSONObject) args[0];

            JSONObject matchInfo = data.optJSONObject("matchInfo");
            if (matchInfo != null) {
                String currentTurnPlayer = matchInfo.optString("currentTurnPlayer", "");
                String yourId = matchInfo.optString("yourId", game.myUserId);
                game.esMiTurno = currentTurnPlayer.equals(yourId);
                Log.d(TAG, "Turno inicial: " + game.esMiTurno);
            }

            int ammo = data.optInt("ammo", 0);
            int fuel = data.optInt("fuel", 0);
            if (game.listener != null) {
                game.listener.onRecursosActualizados(fuel, ammo);
            }

            game.flota.clear();

            Map<String, UserShip> diccionarioPartida = new HashMap<>();
            HashSet<String> userShipsYaUsados = new HashSet<>();

            JSONArray playerFleet = data.optJSONArray("playerFleet");

            JSONArray enemyFleet = data.optJSONArray("enemyFleet");
            if (enemyFleet == null) {
                enemyFleet = data.optJSONArray("visibleEnemyFleet");
            }

            if (playerFleet != null) {
                for (int i = 0; i < playerFleet.length(); i++) {
                    JSONObject s = playerFleet.getJSONObject(i);

                    UserShip matchShip = resolverUserShipParaBarcoPartida(s, userShipsYaUsados);
                    BarcoLogico barco = construirBarcoDesdeJson(s, true, matchShip);

                    game.flota.add(barco);

                    Log.d(TAG, "Barco partida " + s.getString("id")
                            + " pos=(" + s.optInt("x", -1) + "," + s.optInt("y", -1) + ")"
                            + " orient=" + s.optString("orientation", "?")
                            + " hpActual=" + s.optInt("currentHp", -1)
                            + " visionRange=" + s.optInt("visionRange", -1)
                            + " -> inventario="
                            + (matchShip != null ? matchShip.getId() : "null")
                            + " template="
                            + (matchShip != null && matchShip.getShipTemplate() != null
                            ? matchShip.getShipTemplate().getSlug()
                            : "null")
                            + " tam="
                            + (matchShip != null ? calcularTamanoDesdeTemplate(matchShip) : -1));

                    if (matchShip != null) {
                        diccionarioPartida.put(barco.id, matchShip);

                        if (matchShip.getWeaponTemplates() != null) {
                            StringBuilder armas = new StringBuilder();
                            for (UserShip.WeaponItem w : matchShip.getWeaponTemplates()) {
                                if (w != null && w.slug != null) {
                                    if (armas.length() > 0) armas.append(", ");
                                    armas.append(w.slug);
                                }
                            }
                            Log.d(TAG, "Armas asociadas a " + s.getString("id") + ": " + armas);
                        } else {
                            Log.d(TAG, "Armas asociadas a " + s.getString("id") + ": ninguna");
                        }
                    }
                }
            }

            if (enemyFleet != null) {
                for (int i = 0; i < enemyFleet.length(); i++) {
                    JSONObject s = enemyFleet.getJSONObject(i);
                    game.flota.add(construirBarcoDesdeJson(s, false, null));
                }
            }

            game.diccionarioFlota.clear();
            game.diccionarioFlota.putAll(diccionarioPartida);

            // Extraemos los arrays de proyectiles del JSON inicial
            JSONArray proyPropios = data.optJSONArray("proyPropios");
            JSONArray proyEnemigos = data.optJSONArray("proyEnemigos");

            // Reutilizamos el método del GestorJuego para inyectarlos en el tablero
            game.sincronizarProyectilesVision(proyPropios, proyEnemigos);

            if (game.listener != null) {
                game.listener.onSnapshotCompleto(); // Al llamar a esto, el tablero se pintará ya con los torpedos cargados
            }
        } catch (Exception e) {
            Log.e(TAG, "Error procesando match:startInfo", e);
        }
    }

    private UserShip resolverUserShipParaBarcoPartida(JSONObject s,
                                                      HashSet<String> userShipsYaUsados) throws JSONException {
        if (game.inventarioOriginal == null || game.inventarioOriginal.isEmpty()) return null;

        int hpActualPartida = s.optInt("currentHp", 1);

        for (Map.Entry<String, UserShip> entry : game.inventarioOriginal.entrySet()) {
            String userShipId = entry.getKey();
            UserShip uShip = entry.getValue();

            if (userShipsYaUsados.contains(userShipId)) continue;
            if (uShip == null || uShip.getShipTemplate() == null) continue;

            if (uShip.getShipTemplate().getBaseMaxHp() == hpActualPartida) {
                userShipsYaUsados.add(userShipId);
                return uShip;
            }
        }

        for (Map.Entry<String, UserShip> entry : game.inventarioOriginal.entrySet()) {
            String userShipId = entry.getKey();
            UserShip uShip = entry.getValue();

            if (userShipsYaUsados.contains(userShipId)) continue;
            if (uShip == null) continue;

            userShipsYaUsados.add(userShipId);
            return uShip;
        }

        return null;
    }

    public BarcoLogico construirBarcoDesdeJson(JSONObject s,
                                               boolean esAliado,
                                               UserShip uShipVinculado) throws JSONException {
        String idPartida = s.getString("id");

        BarcoLogico previo = game.obtenerBarcoPorId(idPartida);

        int tamanoReal = 1;
        String slugReal = null;
        int hpMaxReal = s.optInt("currentHp", 1);

        if (previo != null) {
            tamanoReal = previo.tipo;
            slugReal = previo.slug;
            hpMaxReal = previo.hpMax;
        } else if (uShipVinculado != null && uShipVinculado.getShipTemplate() != null) {
            tamanoReal = calcularTamanoDesdeTemplate(uShipVinculado);
            slugReal = uShipVinculado.getShipTemplate().getSlug();
            hpMaxReal = uShipVinculado.getShipTemplate().getBaseMaxHp();
        } else {
            UserShip inferido = inferirTemplatePorHpMaxExacta(s.optInt("currentHp", 1));
            if (inferido != null && inferido.getShipTemplate() != null) {
                tamanoReal = calcularTamanoDesdeTemplate(inferido);
                slugReal = inferido.getShipTemplate().getSlug();
                hpMaxReal = inferido.getShipTemplate().getBaseMaxHp();
            }
        }

        try {
            String orientacionRecibida = s.optString("orientation", "NO_RECIBIDA");
            Log.d("DEBUG_ORIENTACION", "🚢 Mapeando barco. JSON recibido: " + s.toString());
            Log.d("DEBUG_ORIENTACION", "🧭 Orientación detectada por el Mapper: " + orientacionRecibida);
        } catch (Exception e) {
            Log.e("DEBUG_ORIENTACION", "Error leyendo JSON del barco", e);
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
        barco.hpMax = hpMaxReal;

        // NUEVO: rango de visión real desde backend
        barco.visionRange = s.optInt("visionRange", -1);

        JSONArray weaponsArray = s.optJSONArray("weapons");
        if (weaponsArray != null) {
            for (int i = 0; i < weaponsArray.length(); i++) {
                JSONObject w = weaponsArray.optJSONObject(i);
                if (w != null) {
                    barco.armas.add(w.optString("type")); // Guardará "CANNON", "TORPEDO" o "MINE"
                }
            }
        }

        return barco;
    }

    private UserShip inferirTemplatePorHpMaxExacta(int hp) {
        if (game.inventarioOriginal == null || game.inventarioOriginal.isEmpty()) return null;

        for (UserShip uShip : game.inventarioOriginal.values()) {
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
}