package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.network.SocketManager;

import org.json.JSONObject;

import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG_AI";
    private static final String TAG_RECONEXION = "RECONEXION";

    private View btnCompetitivo, btnUnirse, btnConfigurarFlota, btnPerfil, btnAjustes, btnPractica;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCompetitivo = findViewById(R.id.btnCompetitivo);
        btnUnirse = findViewById(R.id.btnUnirse);
        btnConfigurarFlota = findViewById(R.id.btnConfigFlota);
        btnPerfil = findViewById(R.id.btnProfile);
        btnPractica = findViewById(R.id.btnPractica);
        btnAjustes = findViewById(R.id.btnSettings);

        inicializarSocketSiHaySesion();

        // --- BOTÓN COMPETITIVO / IA ---
        if (btnCompetitivo != null) {
            btnCompetitivo.setOnClickListener(v -> {
                if (isUsuarioLogueado()) {
                    SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                    String userId = prefs.getString("userId", "default");
                    boolean tieneMazo = prefs.getBoolean("flota_guardada_" + userId, false);

                    if (!tieneMazo) {
                        mostrarAvisoMazoVacio();
                        return;
                    }

                    if (mSocket == null) {
                        AppNotifier.show(MainActivity.this, "No se pudo conectar con el servidor", AppNotifier.Type.ERROR);
                        inicializarSocketSiHaySesion();
                        return;
                    }

                    try {
                        AppNotifier.show(MainActivity.this, "Buscando partida contra IA...", AppNotifier.Type.INFO);
                        mSocket.emit("game:play_bot", new JSONObject());
                        Log.d(TAG, "Emit game:play_bot -> {}");
                    } catch (Exception e) {
                        Log.e(TAG, "Error lanzando partida contra IA", e);
                        AppNotifier.show(MainActivity.this, "Error al iniciar la partida contra IA", AppNotifier.Type.ERROR);
                    }

                } else {
                    mostrarDialogoAlistamiento();
                }
            });
        }

        // --- BOTÓN UNIRSE ---
        if (btnUnirse != null) {
            btnUnirse.setOnClickListener(v -> {
                if (isUsuarioLogueado()) {
                    SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                    String userId = prefs.getString("userId", "default");
                    boolean tieneMazo = prefs.getBoolean("flota_guardada_" + userId, false);

                    if (tieneMazo) {
                        startActivity(new Intent(MainActivity.this, PantallaUnirse.class));
                    } else {
                        mostrarAvisoMazoVacio();
                    }
                } else {
                    mostrarDialogoAlistamiento();
                }
            });
        }

        // --- BOTÓN PRÁCTICA / CREAR PARTIDA NORMAL ---
        if (btnPractica != null) {
            btnPractica.setOnClickListener(v -> {
                if (isUsuarioLogueado()) {
                    SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                    String userId = prefs.getString("userId", "default");
                    boolean tieneMazo = prefs.getBoolean("flota_guardada_" + userId, false);

                    if (tieneMazo) {
                        startActivity(new Intent(MainActivity.this, LobbyActivity.class));
                    } else {
                        mostrarAvisoMazoVacio();
                    }
                } else {
                    mostrarDialogoAlistamiento();
                }
            });
        }

        // --- BOTÓN CONFIGURAR FLOTA ---
        if (btnConfigurarFlota != null) {
            btnConfigurarFlota.setOnClickListener(v -> {
                if (isUsuarioLogueado()) {
                    startActivity(new Intent(MainActivity.this, ConfigurarFlotaActivity.class));
                } else {
                    mostrarDialogoAlistamiento();
                }
            });
        }

        // --- BOTÓN PERFIL ---
        if (btnPerfil != null) {
            btnPerfil.setOnClickListener(v -> {
                if (isUsuarioLogueado()) {
                    startActivity(new Intent(MainActivity.this, PantallaPerfil.class));
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            });
        }

        // --- BOTÓN AJUSTES ---
        if (btnAjustes != null) {
            btnAjustes.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, PantallaAjustes.class))
            );
        }

        if (isUsuarioLogueado()) {
            iniciarChequeoReconexion();
        }
    }

    private void inicializarSocketSiHaySesion() {
        if (!isUsuarioLogueado()) return;

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        try {
            SocketManager.getInstance().conectar(token);
            mSocket = SocketManager.getInstance().getSocket();
            configurarListenersSocketMain();
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando socket en MainActivity", e);
            mSocket = null;
        }
    }

    private void configurarListenersSocketMain() {
        if (mSocket == null) return;

        mSocket.off("match:ready");
        mSocket.off("game:error");

        mSocket.on("match:ready", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String matchId = data.getString("matchId");

                Log.d(TAG, "match:ready recibido -> matchId=" + matchId);

                Intent intent = new Intent(MainActivity.this, PantallaJuego.class);
                intent.putExtra("MATCH_ID", matchId);
                intent.putExtra("CODIGO_SALA", "");
                intent.putExtra("ES_HOST", true);
                startActivity(intent);

            } catch (Exception e) {
                Log.e(TAG, "Error leyendo match:ready en MainActivity", e);
                AppNotifier.show(MainActivity.this, "No se pudo abrir la partida", AppNotifier.Type.ERROR);
            }
        }));

        mSocket.on("game:error", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String msg = data.optString("message", "Error al iniciar partida");
                AppNotifier.show(MainActivity.this, msg, AppNotifier.Type.ERROR);
            } catch (Exception e) {
                AppNotifier.show(MainActivity.this, "Error al iniciar partida", AppNotifier.Type.ERROR);
            }
        }));
    }

    private void iniciarChequeoReconexion() {
        if (mSocket == null) return;

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token == null) return;

        mSocket.off("game:active_found");
        mSocket.off("game:no_active");
        mSocket.off(Socket.EVENT_CONNECT);

        mSocket.on("game:active_found", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String matchId = data.getString("matchId");

                Log.d(TAG_RECONEXION, "¡Partida activa encontrada! ID: " + matchId);

                Intent intent = new Intent(MainActivity.this, PantallaJuego.class);
                intent.putExtra("MATCH_ID", matchId);
                intent.putExtra("token", token);
                intent.putExtra("esReconexion", true);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e(TAG_RECONEXION, "Error al procesar game:active_found", e);
            }
        }));

        mSocket.on("game:no_active", args ->
                Log.d(TAG_RECONEXION, "No hay partidas pendientes. Menú listo.")
        );

        mSocket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG_RECONEXION, "Socket conectado, enviando game:check_active...");
            mSocket.emit("game:check_active");
        });

        if (mSocket.connected()) {
            Log.d(TAG_RECONEXION, "Socket ya conectado, enviando game:check_active...");
            mSocket.emit("game:check_active");
        }
    }

    private boolean isUsuarioLogueado() {
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        return token != null && !token.isEmpty();
    }

    private void mostrarDialogoAlistamiento() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.setCancelable(true);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnIdentificarse = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("¡ACCESO RESTRINGIDO!");
        tvMensaje.setText("Atención: Necesitas estar alistado en la flota para acceder a esta sección de la Sala de Mando.\n\n¿Deseas identificarte o crear una cuenta ahora?");

        ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#B71C1C"));
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#B71C1C"));

        btnCancelar.setVisibility(android.view.View.VISIBLE);
        btnCancelar.setText("PERMANECER");
        btnIdentificarse.setText("IDENTIFICARSE");

        btnIdentificarse.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void mostrarAvisoMazoVacio() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.setCancelable(true);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnIrAConfigurar = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("¡FLOTA NO PREPARADA!");
        tvMensaje.setText("Comandante, no puedes ir a la batalla sin tus barcos. Por favor, dirígete a los astilleros y configura tu flota antes de buscar partida.");

        ivIcono.setImageResource(android.R.drawable.ic_dialog_info);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#1976D2"));
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#1976D2"));

        btnCancelar.setVisibility(View.VISIBLE);
        btnCancelar.setText("MÁS TARDE");
        btnIrAConfigurar.setText("CONFIGURAR FLOTA");

        btnIrAConfigurar.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, ConfigurarFlotaActivity.class));
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.off("match:ready");
            mSocket.off("game:error");
            mSocket.off("game:active_found");
            mSocket.off("game:no_active");
            mSocket.off(Socket.EVENT_CONNECT);
        }
    }
}