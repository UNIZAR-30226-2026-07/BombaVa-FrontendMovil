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

            if (data.has("playerFleet")) {
                JSONArray fleetArray = data.getJSONArray("playerFleet");
                for (int i = 0; i < fleetArray.length(); i++) {
                    JSONObject s = fleetArray.getJSONObject(i);

                    UserShip matchShip = resolverUserShipParaBarcoPartida(s, userShipsYaUsados);
                    BarcoLogico barco = construirBarcoDesdeJson(s, true, matchShip);

                    game.flota.add(barco);

                    Log.d(TAG, "Barco partida " + s.getString("id")
                            + " pos=(" + s.optInt("x", -1) + "," + s.optInt("y", -1) + ")"
                            + " orient=" + s.optString("orientation", "?")
                            + " hpActual=" + s.optInt("currentHp", -1)
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

            if (data.has("enemyFleet")) {
                JSONArray enemyArray = data.getJSONArray("enemyFleet");
                for (int i = 0; i < enemyArray.length(); i++) {
                    JSONObject s = enemyArray.getJSONObject(i);
                    game.flota.add(construirBarcoDesdeJson(s, false, null));
                }
            }

            game.diccionarioFlota.clear();
            game.diccionarioFlota.putAll(diccionarioPartida);

            if (game.listener != null) {
                game.listener.onSnapshotCompleto();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error procesando match:startInfo", e);
        }
    }

    /**
     * Empareja el barco de partida con el UserShip del inventario.
     *
     * Orden de prioridad:
     * 1) Por tamaño del template (1, 3, 5...)
     * 2) Por hp máximo del template si no encuentra tamaño
     * 3) Primer barco libre como último fallback
     *
     * NO usamos currentHp como criterio principal porque puede variar durante la partida.
     */
    private UserShip resolverUserShipParaBarcoPartida(JSONObject s,
                                                      HashSet<String> userShipsYaUsados) throws JSONException {
        if (game.inventarioOriginal == null || game.inventarioOriginal.isEmpty()) return null;

        int hpActualPartida = s.optInt("currentHp", 1);

        // 1) Inferir tamaño probable desde la vida actual/base conocida del juego
        Integer tamanoProbable = inferirTamanoDesdeHp(hpActualPartida);

        if (tamanoProbable != null) {
            for (Map.Entry<String, UserShip> entry : game.inventarioOriginal.entrySet()) {
                String userShipId = entry.getKey();
                UserShip uShip = entry.getValue();

                if (userShipsYaUsados.contains(userShipId)) continue;
                if (uShip == null || uShip.getShipTemplate() == null) continue;

                int tam = calcularTamanoDesdeTemplate(uShip);
                if (tam == tamanoProbable) {
                    userShipsYaUsados.add(userShipId);
                    return uShip;
                }
            }
        }

        // 2) Fallback por baseMaxHp del template
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

        // 3) Último fallback: el primero libre
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
        if (game.inventarioOriginal == null || game.inventarioOriginal.isEmpty()) return null;

        // Primero intentar por hp exacto
        for (UserShip uShip : game.inventarioOriginal.values()) {
            if (uShip != null &&
                    uShip.getShipTemplate() != null &&
                    uShip.getShipTemplate().getBaseMaxHp() == hp) {
                return uShip;
            }
        }

        // Si no, intentar por tamaño probable
        Integer tamanoProbable = inferirTamanoDesdeHp(hp);
        if (tamanoProbable != null) {
            for (UserShip uShip : game.inventarioOriginal.values()) {
                if (uShip != null && uShip.getShipTemplate() != null) {
                    int tam = calcularTamanoDesdeTemplate(uShip);
                    if (tam == tamanoProbable) {
                        return uShip;
                    }
                }
            }
        }

        return null;
    }

    private Integer inferirTamanoDesdeHp(int hp) {
        // Ajustado a tus templates actuales:
        // lancha: hp 20, tamaño 1
        // fragata: hp 30, tamaño 3
        // acorazado: hp 50, tamaño 5
        //
        // Si el barco está dañado, usamos rangos simples.
        if (hp <= 20) return 1;
        if (hp <= 30) return 3;
        return 5;
    }

    private int calcularTamanoDesdeTemplate(UserShip userShip) {
        if (userShip == null || userShip.getShipTemplate() == null) return 1;
        int width = userShip.getShipTemplate().getWidth();
        int height = userShip.getShipTemplate().getHeight();
        return Math.max(width, height);
    }
}