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
import android.widget.Toast;

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
import java.util.List;

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
    private String idMazoActivo = null;

    private LinearLayout layoutControlesColocacion, layoutControlesArmas;
    private Button btnRotar, btnGuardarArma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_flota);
        gestorLogica = new GestorConfiguracionFlota();
        vincularVistasYListeners();
        configurarTablero();
        descargarInventarioDelCuartelGeneral();

        ImageView btnInfoLeyenda = findViewById(R.id.btn_info_leyenda);
        btnInfoLeyenda.setOnClickListener(v -> mostrarDialogoLeyenda());
    }

    private void vincularVistasYListeners() {
        layoutControlesColocacion = findViewById(R.id.layout_controles);
        layoutControlesArmas = findViewById(R.id.layout_controles_armas);
        btnRotar = findViewById(R.id.btn_rotate);
        btnGuardarArma = findViewById(R.id.btn_guardar_arma);

        Button btnShip5 = findViewById(R.id.btn_ship_5x1), btnShip3 = findViewById(R.id.btn_ship_3x1), btnShip1 = findViewById(R.id.btn_ship_1x1);
        uiHelper = new BotonesUIHelper(btnShip5, btnShip3, btnShip1, findViewById(R.id.btn_ametralladora), findViewById(R.id.btn_misil), findViewById(R.id.btn_torpedo));

        btnShip5.setOnClickListener(v -> { tamanoSeleccionado = 5; uiHelper.resaltarBarco(5); });
        btnShip3.setOnClickListener(v -> { tamanoSeleccionado = 3; uiHelper.resaltarBarco(3); });
        btnShip1.setOnClickListener(v -> { tamanoSeleccionado = 1; uiHelper.resaltarBarco(1); });
        btnRotar.setOnClickListener(v -> { enHorizontal = !enHorizontal; btnRotar.setText(enHorizontal ? "Rotar (H)" : "Rotar (V)"); });

        findViewById(R.id.btn_ametralladora).setOnClickListener(v -> seleccionarArmaTemporal("Ametralladora"));
        findViewById(R.id.btn_misil).setOnClickListener(v -> seleccionarArmaTemporal("Misil"));
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

    private void mostrarDialogoLeyenda() {
        // Crear el diálogo
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_leyenda_mapa);

        // Hacer que el fondo sea transparente para que se vean las esquinas redondeadas de tu pergamino
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            // Opcional: hacer que el diálogo sea un poco más ancho
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Configurar el botón de cerrar
        Button btnCerrar = dialog.findViewById(R.id.btn_cerrar_leyenda);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        // Mostrarlo en pantalla
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
            tamanoSeleccionado = 0; uiHelper.resaltarBarco(0);
            uiHelper.ocultarBarcosColocados(gestorLogica.estaBarcoColocado(5), gestorLogica.estaBarcoColocado(3), gestorLogica.estaBarcoColocado(1));
            adaptador.notifyDataSetChanged();

            // 🔥 NUEVO: EJECUTAR VIBRACIÓN AL COLOCAR EL BARCO
            ejecutarVibracion(50); // Vibra 50 milisegundos

        } else {
            Toast.makeText(this, "Colocación inválida", Toast.LENGTH_SHORT).show();
            ejecutarVibracion(150); // Vibración más larga si hay un error
        }
    }

    // Añade este método en cualquier parte de ConfigurarFlotaActivity
    private void ejecutarVibracion(int milisegundos) {
        // 1. Leemos los ajustes
        SharedPreferences prefs = getSharedPreferences("AjustesApp", MODE_PRIVATE);
        boolean vibracionActivada = prefs.getBoolean("vibracion_activada", true);

        // 2. Si está activada, hacemos vibrar el móvil
        if (vibracionActivada) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            Log.d("BOMBAVA_VIBRACION", "¡Bzz Bzz! El móvil está vibrando por " + milisegundos + " ms");
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Para móviles más antiguos
                    v.vibrate(milisegundos);
                }
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
        uiHelper.actualizarBotonesArmas(gestorLogica.getTamanoBarco(id), gestorLogica.tieneArmaEquipada(id, "Ametralladora"), gestorLogica.tieneArmaEquipada(id, "Misil"), gestorLogica.tieneArmaEquipada(id, "Torpedo"));
        layoutControlesArmas.setVisibility(View.VISIBLE);
        adaptador.notifyDataSetChanged();
    }

    private void seleccionarArmaTemporal(String tipo) {
        armaTemporal = tipo;
        uiHelper.resaltarArma(tipo);
        btnGuardarArma.setText(gestorLogica.tieneArmaEquipada(idBarcoSeleccionadoParaArma, tipo) ? "Desequipar" : "Equipar");
    }

    private void guardarArmaBarco() {
        if (armaTemporal.isEmpty()) return;
        if (gestorLogica.tieneArmaEquipada(idBarcoSeleccionadoParaArma, armaTemporal)) gestorLogica.desequiparArma(idBarcoSeleccionadoParaArma, armaTemporal);
        else gestorLogica.equiparArma(idBarcoSeleccionadoParaArma, armaTemporal);
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
            } else Toast.makeText(this, "Faltan barcos por colocar", Toast.LENGTH_SHORT).show();
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
                    inventarioBarcos = response.body(); // Guardamos el inventario completo

                    for (UserShip b : inventarioBarcos) {
                        int t = b.getShipTemplate().getTamanoCasillas();
                        if (t == 5) realIdBarco5 = b.getId();
                        else if (t == 3) realIdBarco3 = b.getId();
                        else if (t == 1) realIdBarco1 = b.getId();
                    }
                    cargarFlotaExistente();
                }
            }
            @Override public void onFailure(Call<List<UserShip>> call, Throwable t) {
                Log.e("FLOTA_DEBUG", "Fallo red al inventario: " + t.getMessage());
            }
        });
    }

    private void cargarFlotaExistente() {
        String token = getSharedPreferences("BOMBA_VA", MODE_PRIVATE).getString("token", "");

        ApiClient.getApiService().obtenerMazo("Bearer " + token).enqueue(new Callback<List<FleetConfigRequest>>() {
            @Override
            public void onResponse(Call<List<FleetConfigRequest>> call, Response<List<FleetConfigRequest>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    gestorLogica.resetearTablero();
                    List<FleetConfigRequest> listaMazos = response.body();
                    FleetConfigRequest mazoCargar = null;

                    for (FleetConfigRequest mazo : listaMazos) {
                        if (mazo.isActive()) { mazoCargar = mazo; break; } // Usando isActive como en la v final
                    }
                    if (mazoCargar == null) mazoCargar = listaMazos.get(listaMazos.size() - 1);

                    if (mazoCargar.getShipPositions() != null) {
                        for (ShipPosition p : mazoCargar.getShipPositions()) {
                            // Cargar la posición del barco
                            gestorLogica.cargarBarcoDesdeServidor(p.getUserShipId(), p.getPosition().getX(), p.getPosition().getY(), p.getOrientation(), realIdBarco5, realIdBarco3, realIdBarco1);

                            int idInternoRecienCreado = gestorLogica.getIdBarcoActual() - 1;

                            for (UserShip barcoInventario : inventarioBarcos) {
                                if (barcoInventario.getId().equals(p.getUserShipId())) {

                                    // ¡Ahora sí reconocerá getWeaponSlug()!
                                    String slugArma = barcoInventario.getWeaponSlug();

                                    // 🔥 AÑADE ESTA LÍNEA AQUÍ:
                                    Log.d("FLOTA_WEAPON", "Barco ID: " + barcoInventario.getId() + " | Arma leída: " + slugArma);

                                    if (slugArma != null && !slugArma.isEmpty()) {
                                        if (slugArma.toLowerCase().contains("ametralladora")) {
                                            gestorLogica.equiparArma(idInternoRecienCreado, "Ametralladora");
                                        } else if (slugArma.toLowerCase().contains("misil")) {
                                            gestorLogica.equiparArma(idInternoRecienCreado, "Misil");
                                        } else if (slugArma.toLowerCase().contains("torpedo")) {
                                            gestorLogica.equiparArma(idInternoRecienCreado, "Torpedo");
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    actualizarTodaLaVistaTablero();
                }
            }
            @Override public void onFailure(Call<List<FleetConfigRequest>> call, Throwable t) {}
        });
    }

    private void enviarFlotaAlServidor() {
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        // 🔥 TRUCO CLAVE: Como no podemos actualizar un mazo, creamos uno NUEVO cada vez que el usuario guarda.
        // Usamos el tiempo actual en milisegundos para garantizar que el nombre nunca se repita y el backend no dé error.
        final String nombreMazoUnico = "Flota Activa " + System.currentTimeMillis();

        List<ShipPosition> posiciones = gestorLogica.obtenerPosicionesParaBackend(realIdBarco5, realIdBarco3, realIdBarco1);

        // Corrección de orientación para el backend
        for(ShipPosition sp : posiciones) {
            if(sp.getOrientation().equals("S")) sp.setOrientation("N");
        }

        FleetConfigRequest request = new FleetConfigRequest(nombreMazoUnico, posiciones);

        // PASO 1: CREAR EL NUEVO MAZO
        ApiClient.getApiService().crearMazoFlota("Bearer " + token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ConfigurarFlotaActivity.this, "Error guardando la configuración", Toast.LENGTH_SHORT).show();
                    return;
                }

                // PASO 2: BUSCAR EL ID DEL MAZO QUE ACABAMOS DE CREAR
                ApiClient.getApiService().obtenerMazo("Bearer " + token).enqueue(new Callback<List<FleetConfigRequest>>() {
                    @Override
                    public void onResponse(Call<List<FleetConfigRequest>> call, Response<List<FleetConfigRequest>> res) {
                        if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                            String idMazoNuevo = null;

                            // Buscamos el mazo por el nombre único que le acabamos de poner
                            for (FleetConfigRequest mazo : res.body()) {
                                if (nombreMazoUnico.equals(mazo.getName())) {
                                    idMazoNuevo = mazo.getId();
                                    break;
                                }
                            }

                            // Por si falla el nombre, cogemos el último insertado
                            if (idMazoNuevo == null) {
                                idMazoNuevo = res.body().get(res.body().size() - 1).getId();
                            }

                            // PASO 3: ACTIVAR EL NUEVO MAZO
                            activarMazoConUUID(token, idMazoNuevo);
                        }
                    }
                    @Override public void onFailure(Call<List<FleetConfigRequest>> call, Throwable t) {}
                });
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void activarMazoConUUID(String token, String deckId) {
        ApiClient.getApiService().activarMazo("Bearer " + token, deckId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Si se activa bien, mandamos las armas
                    enviarArmasAlServidor(token);
                    Toast.makeText(ConfigurarFlotaActivity.this, "¡Configuración actualizada!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ConfigurarFlotaActivity.this, "Error al activar el mazo", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void enviarArmasAlServidor(String token) {
        for (int i = 1; i < gestorLogica.getIdBarcoActual(); i++) {
            int t = gestorLogica.getTamanoBarco(i);
            String rid = (t == 5) ? realIdBarco5 : (t == 3 ? realIdBarco3 : realIdBarco1);

            // Evaluamos qué arma tiene
            String slug = gestorLogica.tieneArmaEquipada(i, "Ametralladora") ? "ametralladora" :
                    (gestorLogica.tieneArmaEquipada(i, "Misil") ? "misil" :
                            (gestorLogica.tieneArmaEquipada(i, "Torpedo") ? "torpedo" : ""));

            // 🔥 CAMBIO CRÍTICO PARA DESEQUIPAR:
            // Si slug está vacío, enviamos null (o "") para decirle al backend "quita el arma".
            String valorAEnviar = slug.isEmpty() ? null : slug;

            ApiClient.getApiService().equiparArma(rid, "Bearer " + token, new EquipWeaponRequest(valorAEnviar)).enqueue(new Callback<UserShip>() {
                @Override public void onResponse(Call<UserShip> call, Response<UserShip> response) {
                    Log.d("FLOTA_DEBUG", "Arma actualizada para barco " + rid + " -> " + valorAEnviar);
                }
                @Override public void onFailure(Call<UserShip> call, Throwable t) {}
            });
        }
    }

    private void actualizarTodaLaVistaTablero() {
        for (int i = 0; i < 225; i++) celdasTablero.get(i).idImagenBarco = 0;
        for (int id = 1; id < gestorLogica.getIdBarcoActual(); id++) {
            int inicio = -1;
            for (int p = 0; p < 225; p++) { if (gestorLogica.getIdBarcoEn(p) == id) { inicio = p; break; } }
            if (inicio != -1) {
                int tam = gestorLogica.getTamanoBarco(id);
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
}