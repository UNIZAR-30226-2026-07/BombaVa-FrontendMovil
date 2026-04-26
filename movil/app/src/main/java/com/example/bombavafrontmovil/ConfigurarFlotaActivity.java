package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.os.Vibrator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bombavafrontmovil.models.*;
import com.example.bombavafrontmovil.network.ApiClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfigurarFlotaActivity extends AppCompatActivity {

    private String realIdBarco5 = "", realIdBarco3 = "", realIdBarco1 = "";
    private GestorConfiguracionFlota gestorLogica;
    private BotonesUIHelper uiHelper;
    private ConfigurarFlotaAdapter adaptador;
    private List<CeldaVisual> celdasTablero;
    private List<UserShip> inventarioBarcos = new ArrayList<>();

    private boolean enHorizontal = true;
    private int tamanoSeleccionado = 0, faseActual = 1, idBarcoSeleccionadoParaArma = 0;
    private String armaTemporal = "";

    private LinearLayout layoutControlesColocacion, layoutControlesArmas;
    private Button btnRotar, btnGuardarArma;

    private Map<String, Set<String>> armasOriginalesBackend = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_flota);

        gestorLogica = new GestorConfiguracionFlota();

        vincularVistasYListeners();
        configurarTablero();

        descargarInventarioDelCuartelGeneral();
        descargarArmasDisponibles();

        ImageView btnInfoLeyenda = findViewById(R.id.btn_info_leyenda);
        btnInfoLeyenda.setOnClickListener(v -> mostrarDialogoLeyenda());
    }

    private void vincularVistasYListeners() {
        layoutControlesColocacion = findViewById(R.id.layout_controles);
        layoutControlesArmas = findViewById(R.id.layout_controles_armas);
        btnRotar = findViewById(R.id.btn_rotate);
        btnGuardarArma = findViewById(R.id.btn_guardar_arma);

        Button btnShip5 = findViewById(R.id.btn_ship_5x1),
                btnShip3 = findViewById(R.id.btn_ship_3x1),
                btnShip1 = findViewById(R.id.btn_ship_1x1);

        uiHelper = new BotonesUIHelper(btnShip5, btnShip3, btnShip1,
                findViewById(R.id.btn_ametralladora),
                findViewById(R.id.btn_misil),
                findViewById(R.id.btn_torpedo));

        btnShip5.setOnClickListener(v -> { tamanoSeleccionado = 5; uiHelper.resaltarBarco(5); });
        btnShip3.setOnClickListener(v -> { tamanoSeleccionado = 3; uiHelper.resaltarBarco(3); });
        btnShip1.setOnClickListener(v -> { tamanoSeleccionado = 1; uiHelper.resaltarBarco(1); });

        btnRotar.setOnClickListener(v -> {
            enHorizontal = !enHorizontal;
            btnRotar.setText(enHorizontal ? "Rotar (H)" : "Rotar (V)");
        });

        View btnColeccion = findViewById(R.id.btn_coleccion_barcos);
        if (btnColeccion != null) {
            btnColeccion.setOnClickListener(v -> mostrarDialogoColeccion());
        }

        // NUEVAS ARMAS AL HACER CLIC
        findViewById(R.id.btn_ametralladora).setOnClickListener(v -> seleccionarArmaTemporal("Cañón"));
        findViewById(R.id.btn_misil).setOnClickListener(v -> seleccionarArmaTemporal("Mina"));
        findViewById(R.id.btn_torpedo).setOnClickListener(v -> seleccionarArmaTemporal("Torpedo"));

        findViewById(R.id.btn_cancelar_arma).setOnClickListener(v -> cancelarSeleccionArma());
        btnGuardarArma.setOnClickListener(v -> guardarArmaBarco());
        findViewById(R.id.btn_confirmar).setOnClickListener(v -> avanzarFase());
        findViewById(R.id.btn_limpiar_todo).setOnClickListener(v -> limpiarTableroCompleto());
    }

    private void configurarTablero() {
        RecyclerView rvTablero = findViewById(R.id.rvBoard);
        celdasTablero = new ArrayList<>();
        int colEnemiga = Color.parseColor("#90CAF9"), colNeutra = Color.parseColor("#4FC3F7"), colAliada = ContextCompat.getColor(this, R.color.sea_blue);
        for (int i = 0; i < 225; i++) celdasTablero.add(new CeldaVisual((i / 15 < 5) ? colEnemiga : ((i / 15 < 10) ? colNeutra : colAliada)));
        rvTablero.setLayoutManager(new GridLayoutManager(this, 15));
        adaptador = new ConfigurarFlotaAdapter(celdasTablero, this::manejarToqueCelda);
        rvTablero.setAdapter(adaptador);
    }

    private void mostrarDialogoColeccion() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_coleccion_barcos);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Configurar el botón de cerrar
        dialog.findViewById(R.id.btn_cerrar_coleccion).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void mostrarDialogoLeyenda() {
        android.app.Dialog dialog = new android.app.Dialog(this);

        if (faseActual == 1) {
            // LEYENDA DEL MAPA (Colocación de barcos)
            dialog.setContentView(R.layout.dialog_leyenda_mapa);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.findViewById(R.id.btn_cerrar_leyenda).setOnClickListener(v -> dialog.dismiss());

        } else {
            // LEYENDA DE ARMAS (Equipamiento)
            dialog.setContentView(R.layout.dialog_leyenda_armas);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.findViewById(R.id.btn_cerrar_leyenda_armas).setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void manejarToqueCelda(int pos) {
        int idB = gestorLogica.getIdBarcoEn(pos);
        if (faseActual == 2) {
            if (idB != 0) abrirPanelArmas(idB);
        } else {
            intentarColocarOQuitarBarco(pos, idB);
        }
    }

    private void intentarColocarOQuitarBarco(int pos, int idExistente) {
        if (idExistente != 0) {
            if (tamanoSeleccionado == 0) borrarBarcoUI(idExistente);
            return;
        }
        if (tamanoSeleccionado == 0) return;

        int pAjustada = gestorLogica.ajustarPosicion(pos, tamanoSeleccionado, enHorizontal);

        if (gestorLogica.validarColocacion(pAjustada, tamanoSeleccionado, enHorizontal) == 0) {
            gestorLogica.colocarBarco(pAjustada, tamanoSeleccionado, enHorizontal);
            dibujarBarcoEnVista(pAjustada, tamanoSeleccionado, enHorizontal);
            tamanoSeleccionado = 0;
            uiHelper.resaltarBarco(0);
            uiHelper.ocultarBarcosColocados(gestorLogica.estaBarcoColocado(5), gestorLogica.estaBarcoColocado(3), gestorLogica.estaBarcoColocado(1));
            adaptador.notifyDataSetChanged();
            ejecutarVibracion(50);
        } else {
            AppNotifier.show(this, "Colocación inválida", AppNotifier.Type.ERROR);
            ejecutarVibracion(150);
        }
    }

    private void ejecutarVibracion(int milisegundos) {
        SharedPreferences prefs = getSharedPreferences("AjustesApp", MODE_PRIVATE);
        if (prefs.getBoolean("vibracion_activada", true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(milisegundos);
            }
        }
    }

    private void dibujarBarcoEnVista(int pos, int tam, boolean hor) {
        for (int i = 0; i < tam; i++) {
            CeldaVisual c = celdasTablero.get(hor ? (pos + i) : (pos + (i * 15)));
            c.rotacion = hor ? 90f : 0f;
            if (tam == 1) c.idImagenBarco = R.drawable.barco_medio;
            else if (i == 0) c.idImagenBarco = hor ? R.drawable.barco_popa : R.drawable.barco_proa;
            else if (i == tam - 1) c.idImagenBarco = hor ? R.drawable.barco_proa : R.drawable.barco_popa;
            else c.idImagenBarco = R.drawable.barco_medio;
        }
    }

    private void borrarBarcoUI(int id) {
        for (int i = 0; i < 225; i++) { if (gestorLogica.getIdBarcoEn(i) == id) celdasTablero.get(i).idImagenBarco = 0; }
        gestorLogica.borrarBarco(id);
        uiHelper.ocultarBarcosColocados(gestorLogica.estaBarcoColocado(5), gestorLogica.estaBarcoColocado(3), gestorLogica.estaBarcoColocado(1));
        adaptador.notifyDataSetChanged();
    }

    private void abrirPanelArmas(int id) {
        idBarcoSeleccionadoParaArma = id;
        for (int i = 0; i < 225; i++) celdasTablero.get(i).seleccionadaParaArma = (gestorLogica.getIdBarcoEn(i) == id);

        int tamano = gestorLogica.getTamanoBarco(id);
        Set<String> equipadas = gestorLogica.getArmasEquipadas(id);
        int huecosLibres = tamano - equipadas.size();

        ((TextView) findViewById(R.id.tv_title)).setText("ARMAS: " + equipadas.size() + "/" + tamano + " EQUIPADAS");

        // MAPEO DE BOTONES A LAS NUEVAS ARMAS
        configurarBotonArma(findViewById(R.id.btn_ametralladora), "Cañón", equipadas, huecosLibres);
        configurarBotonArma(findViewById(R.id.btn_misil), "Mina", equipadas, huecosLibres);
        configurarBotonArma(findViewById(R.id.btn_torpedo), "Torpedo", equipadas, huecosLibres);

        armaTemporal = "";
        btnGuardarArma.setText("SELECCIONA");
        btnGuardarArma.setEnabled(false);

        layoutControlesArmas.setVisibility(View.VISIBLE);
        layoutControlesColocacion.setVisibility(View.GONE);
        adaptador.notifyDataSetChanged();
    }

    private void configurarBotonArma(Button btn, String armaBase, Set<String> equipadas, int huecosLibres) {
        String textoCorto = armaBase.substring(0, Math.min(6, armaBase.length())).toUpperCase();
        if (equipadas.contains(armaBase)) {
            btn.setVisibility(View.VISIBLE);
            btn.setText("✅ " + textoCorto);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#388E3C")));
        } else {
            btn.setText(textoCorto);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8D6E63")));
            btn.setVisibility(huecosLibres <= 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void seleccionarArmaTemporal(String tipo) {
        armaTemporal = tipo;
        uiHelper.resaltarArma(tipo);
        btnGuardarArma.setEnabled(true);
        if (gestorLogica.tieneArmaEquipada(idBarcoSeleccionadoParaArma, tipo)) {
            btnGuardarArma.setText("DESEQUIPAR");
            btnGuardarArma.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        } else {
            btnGuardarArma.setText("EQUIPAR");
            btnGuardarArma.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#388E3C")));
        }
    }

    private void guardarArmaBarco() {
        if (armaTemporal.isEmpty()) return;
        if (gestorLogica.tieneArmaEquipada(idBarcoSeleccionadoParaArma, armaTemporal)) {
            gestorLogica.desequiparArma(idBarcoSeleccionadoParaArma, armaTemporal);
            AppNotifier.show(this, "Arma retirada", AppNotifier.Type.INFO);
        } else {
            gestorLogica.equiparArma(idBarcoSeleccionadoParaArma, armaTemporal);
            AppNotifier.show(this, "Arma equipada", AppNotifier.Type.SUCCESS);
        }
        ejecutarVibracion(50);
        abrirPanelArmas(idBarcoSeleccionadoParaArma);
    }

    private void cancelarSeleccionArma() {
        layoutControlesArmas.setVisibility(View.GONE);
        for (CeldaVisual c : celdasTablero) c.seleccionadaParaArma = false;
        adaptador.notifyDataSetChanged();
    }

    private void avanzarFase() {
        if (faseActual == 1) {
            if (gestorLogica.estaBarcoColocado(5) && gestorLogica.estaBarcoColocado(3) && gestorLogica.estaBarcoColocado(1)) {
                faseActual = 2;
                layoutControlesColocacion.setVisibility(View.GONE);
                ((TextView)findViewById(R.id.tv_title)).setText("Modifica tus Armas");
            } else {
                AppNotifier.show(this, "Faltan barcos por colocar", AppNotifier.Type.ERROR);
            }
        } else {
            enviarFlotaAlServidor();
        }
    }

    private void descargarInventarioDelCuartelGeneral() {
        String token = getSharedPreferences("BOMBA_VA", MODE_PRIVATE).getString("token", "");

        ApiClient.getApiService().obtenerInventarioBarcos("Bearer " + token).enqueue(new Callback<List<UserShip>>() {
            @Override
            public void onResponse(Call<List<UserShip>> call, Response<List<UserShip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inventarioBarcos = response.body();

                    for (UserShip b : inventarioBarcos) {
                        int t = 0;
                        if (b.getShipTemplate() != null) t = b.getShipTemplate().getTamanoCasillas();

                        if (t == 5) realIdBarco5 = b.getId();
                        else if (t == 3) realIdBarco3 = b.getId();
                        else if (t == 1) realIdBarco1 = b.getId();

                        List<String> armasDetectadas = new ArrayList<>();

                        // LEEMOS EL CAJÓN "WeaponTemplates"
                        if (b.getWeaponTemplates() != null) {
                            for (UserShip.WeaponItem item : b.getWeaponTemplates()) {
                                if (item.slug != null) {
                                    armasDetectadas.add(item.slug);
                                }
                            }
                        }

                        if (b.getWeaponSlug() != null) armasDetectadas.add(b.getWeaponSlug());
                        if (b.getWeaponSlugs() != null) armasDetectadas.addAll(b.getWeaponSlugs());
                        if (b.getWeaponsList() != null) {
                            for (UserShip.WeaponItem item : b.getWeaponsList()) if (item.slug != null) armasDetectadas.add(item.slug);
                        }
                        if (b.getShipWeapons() != null) {
                            for (UserShip.WeaponItem item : b.getShipWeapons()) if (item.slug != null) armasDetectadas.add(item.slug);
                        }
                        if (b.getEquipped() != null) {
                            for (UserShip.WeaponItem item : b.getEquipped()) if (item.slug != null) armasDetectadas.add(item.slug);
                        }

                        Log.d("FLOTA_DEBUG", "Barco " + b.getId() + " | Armas finales leídas: " + armasDetectadas.toString());
                        if (!armasDetectadas.isEmpty()) {
                            if (!armasOriginalesBackend.containsKey(b.getId())) armasOriginalesBackend.put(b.getId(), new HashSet<>());
                            for (String slugArma : armasDetectadas) armasOriginalesBackend.get(b.getId()).add(slugArma);
                        }
                    }

                    if (realIdBarco5.isEmpty() && inventarioBarcos.size() >= 3) {
                        realIdBarco5 = inventarioBarcos.get(0).getId();
                        realIdBarco3 = inventarioBarcos.get(1).getId();
                        realIdBarco1 = inventarioBarcos.get(2).getId();
                    }
                    cargarFlotaExistente();
                } else {
                    Log.e("FLOTA_DEBUG", "El servidor rechazó el inventario. Código HTTP: " + response.code());
                }
            }
            @Override public void onFailure(Call<List<UserShip>> call, Throwable t) {
                Log.e("FLOTA_DEBUG", "Error de red al inventario: " + t.getMessage());
            }
        });
    }

    // Inicia vacío y solo carga si hay mazo activo
    private void cargarFlotaExistente() {
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        // Extraemos el ID del usuario para saber de quién es el turno
        String userId = prefs.getString("userId", "default");

        // Comprobamos si este usuario ya ha guardado una flota alguna vez en este móvil
        boolean yaConfiguroAntes = prefs.getBoolean("flota_guardada_" + userId, false);

        gestorLogica.resetearTablero(); // Tablero en blanco siempre al inicio

        // Si es su primera vez, ignoramos al servidor y le damos el tablero limpio
        if (!yaConfiguroAntes) {
            actualizarTodaLaVistaTablero();
            sincronizarArmasConUI();
            AppNotifier.show(this, "¡Bienvenido! Es tu primera vez, despliega tu flota.", AppNotifier.Type.INFO);
            return; // Cortamos aquí para que no lea el mazo por defecto del servidor
        }

        // Si ya configuró antes, hacemos la petición normal al servidor
        ApiClient.getApiService().obtenerMazo("Bearer " + token).enqueue(new Callback<List<FleetConfigRequest>>() {
            @Override
            public void onResponse(Call<List<FleetConfigRequest>> call, Response<List<FleetConfigRequest>> response) {
                boolean mazoActivoEncontrado = false;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    FleetConfigRequest mazoCargar = null;

                    for (FleetConfigRequest mazo : response.body()) {
                        if (mazo.isActive()) {
                            mazoCargar = mazo;
                            mazoActivoEncontrado = true;
                            break;
                        }
                    }

                    if (mazoCargar != null && mazoCargar.getShipPositions() != null) {
                        for (ShipPosition p : mazoCargar.getShipPositions()) {
                            gestorLogica.cargarBarcoDesdeServidor(p.getUserShipId(), p.getPosition().getX(), p.getPosition().getY(), p.getOrientation(), realIdBarco5, realIdBarco3, realIdBarco1);
                        }
                    }
                }

                actualizarTodaLaVistaTablero();
                sincronizarArmasConUI();

                if (!mazoActivoEncontrado) {
                    AppNotifier.show(ConfigurarFlotaActivity.this, "Sin flota activa. ¡Despliega tus barcos!", AppNotifier.Type.INFO);
                }
            }
            @Override public void onFailure(Call<List<FleetConfigRequest>> call, Throwable t) {
                actualizarTodaLaVistaTablero();
            }
        });
    }

    private void enviarFlotaAlServidor() {
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        final String nombreMazoUnico = "Flota Activa " + System.currentTimeMillis();
        List<ShipPosition> posiciones = gestorLogica.obtenerPosicionesParaBackend(realIdBarco5, realIdBarco3, realIdBarco1);
        android.util.Log.d("DEBUG_ORIENTACION", "🚀 [PRE-ENVÍO] Posiciones generadas: " + new com.google.gson.Gson().toJson(posiciones));

        if(posiciones.isEmpty()) { AppNotifier.show(this, "Error: No se detectaron barcos para guardar.", AppNotifier.Type.ERROR); return; }

        ApiClient.getApiService().crearMazoFlota("Bearer " + token, new FleetConfigRequest(nombreMazoUnico, posiciones)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) return;
                AppNotifier.show(ConfigurarFlotaActivity.this, "Procesando tácticas...", AppNotifier.Type.INFO);

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    ApiClient.getApiService().obtenerMazo("Bearer " + token).enqueue(new Callback<List<FleetConfigRequest>>() {
                        @Override
                        public void onResponse(Call<List<FleetConfigRequest>> call, Response<List<FleetConfigRequest>> res) {
                            if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                                String idMazoNuevo = null;
                                for (FleetConfigRequest mazo : res.body()) { if (nombreMazoUnico.equals(mazo.getName())) { idMazoNuevo = mazo.getId(); break; } }
                                if (idMazoNuevo == null) idMazoNuevo = res.body().get(res.body().size() - 1).getId();
                                activarMazoConUUID(token, idMazoNuevo);
                            }
                        }
                        @Override public void onFailure(Call<List<FleetConfigRequest>> call, Throwable t) {}
                    });
                }, 1000);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void activarMazoConUUID(String token, String deckId) {
        ApiClient.getApiService().activarMazo("Bearer " + token, deckId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {

                    // Guardamos que este usuario ya tiene una flota real configurada por él
                    SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                    String userId = prefs.getString("userId", "default");
                    prefs.edit().putBoolean("flota_guardada_" + userId, true).apply();

                    enviarArmasAlServidor(token);
                    AppNotifier.show(ConfigurarFlotaActivity.this, "¡Configuración actualizada!", AppNotifier.Type.SUCCESS);
                    finish();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void enviarArmasAlServidor(String token) {
        for (int i = 1; i < gestorLogica.getIdBarcoActual(); i++) {
            int t = gestorLogica.getTamanoBarco(i);
            String rid = (t == 5) ? realIdBarco5 : (t == 3 ? realIdBarco3 : realIdBarco1);

            // 1. Obtenemos las armas de la pantalla (en Español)
            Set<String> armasUI = gestorLogica.getArmasEquipadas(i);

            // 2. Las convertimos al idioma del servidor (Slugs)
            Set<String> slugsNuevos = new HashSet<>();
            for (String uiName : armasUI) {
                if (uiName.equals("Cañón")) slugsNuevos.add("cannon-base");
                else if (uiName.equals("Torpedo")) slugsNuevos.add("torpedo-v1");
                else if (uiName.equals("Mina")) slugsNuevos.add("mine-v1");
            }

            Set<String> slugsViejos = new HashSet<>(armasOriginalesBackend.getOrDefault(rid, new HashSet<>()));

            // 3. EQUIPAR NUEVAS
            for (String slugNuevo : slugsNuevos) {
                if (!slugsViejos.contains(slugNuevo)) {
                    ApiClient.getApiService().equiparArma(rid, "Bearer " + token, new EquipWeaponRequest(slugNuevo)).enqueue(new Callback<UserShip>() {
                        @Override public void onResponse(Call<UserShip> call, Response<UserShip> response) {
                            if (response.isSuccessful()) Log.d("FLOTA_DEBUG", "✅ Arma guardada en el servidor: " + slugNuevo);
                            else Log.e("FLOTA_DEBUG", "❌ Error al guardar arma: " + slugNuevo + " - Código: " + response.code());
                        }
                        @Override public void onFailure(Call<UserShip> call, Throwable t) {
                            Log.e("FLOTA_DEBUG", "❌ Fallo de red guardando arma: " + t.getMessage());
                        }
                    });
                }
            }

            // 4. DESEQUIPAR LAS QUITADAS
            for (String slugViejo : slugsViejos) {
                if (!slugsNuevos.contains(slugViejo)) {
                    ApiClient.getApiService().desequiparArma(rid, slugViejo, "Bearer " + token).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) Log.d("FLOTA_DEBUG", "🗑️ Arma retirada del servidor: " + slugViejo);
                            else Log.e("FLOTA_DEBUG", "❌ Error al retirar arma: " + slugViejo + " - Código: " + response.code());
                        }
                        @Override public void onFailure(Call<Void> call, Throwable t) {
                            Log.e("FLOTA_DEBUG", "❌ Fallo de red retirando arma: " + t.getMessage());
                        }
                    });
                }
            }
        }
    }

    private void actualizarTodaLaVistaTablero() {
        for (int i = 0; i < 225; i++) celdasTablero.get(i).idImagenBarco = 0;
        for (int id = 1; id < gestorLogica.getIdBarcoActual(); id++) {
            int inicio = -1;
            for (int p = 0; p < 225; p++) { if (gestorLogica.getIdBarcoEn(p) == id) { inicio = p; break; } }
            if (inicio != -1) {
                int tam = gestorLogica.getTamanoBarco(id);
                // Tu propia lógica para detectar la orientación:
                boolean hor = (inicio + 1 < 225 && gestorLogica.getIdBarcoEn(inicio + 1) == id);
                dibujarBarcoEnVista(inicio, tam, hor);
            }
        }
        uiHelper.ocultarBarcosColocados(gestorLogica.estaBarcoColocado(5), gestorLogica.estaBarcoColocado(3), gestorLogica.estaBarcoColocado(1));
        adaptador.notifyDataSetChanged();
    }

    private void limpiarTableroCompleto() {
        gestorLogica.resetearTablero();
        for (CeldaVisual c : celdasTablero) { c.idImagenBarco = 0; c.seleccionadaParaArma = false; }
        faseActual = 1;
        uiHelper.ocultarBarcosColocados(false, false, false);
        ((TextView)findViewById(R.id.tv_title)).setText("Configura tu Flota");
        layoutControlesColocacion.setVisibility(View.VISIBLE);
        adaptador.notifyDataSetChanged();
    }

    private void descargarArmasDisponibles() {
        String token = getSharedPreferences("BOMBA_VA", MODE_PRIVATE).getString("token", "");
        ApiClient.getApiService().obtenerArmasDisponibles("Bearer " + token).enqueue(new Callback<List<Weapon>>() {
            @Override
            public void onResponse(Call<List<Weapon>> call, Response<List<Weapon>> response) {}
            @Override public void onFailure(Call<List<Weapon>> call, Throwable t) {}
        });
    }

    private void sincronizarArmasConUI() {
        for (int i = 1; i < gestorLogica.getIdBarcoActual(); i++) {
            int t = gestorLogica.getTamanoBarco(i);
            String rid = (t == 5) ? realIdBarco5 : (t == 3 ? realIdBarco3 : realIdBarco1);

            Set<String> armasDelBackend = armasOriginalesBackend.getOrDefault(rid, new HashSet<>());

            for (String slug : armasDelBackend) {
                String nombreUI = "";

                // TRADUCTOR: Del idioma del servidor al de la pantalla
                if (slug.equalsIgnoreCase("cannon-base") || slug.equalsIgnoreCase("Cañón") || slug.equalsIgnoreCase("Canon")) {
                    nombreUI = "Cañón";
                } else if (slug.equalsIgnoreCase("mine-v1") || slug.equalsIgnoreCase("Mina")) {
                    nombreUI = "Mina";
                } else if (slug.equalsIgnoreCase("torpedo-v1") || slug.equalsIgnoreCase("Torpedo")) {
                    nombreUI = "Torpedo";
                }

                // Si lo reconoce, lo inyecta en la lógica visual para pintar de verde
                if (!nombreUI.isEmpty()) {
                    gestorLogica.equiparArma(i, nombreUI);
                }
            }
        }
    }
}