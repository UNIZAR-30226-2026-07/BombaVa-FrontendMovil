package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.models.UserShip;
import com.example.bombavafrontmovil.network.ApiClient;
import com.example.bombavafrontmovil.network.SocketManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PantallaJuego extends AppCompatActivity {

    private static final String TAG = "DEBUG_BOMBA";

    private String token;
    private String matchId;
    private String myUserId;
    private String codigoSala;
    private boolean esHost;

    private GestorJuego gestor;
    private PantallaJuegoUi ui;
    private PantallaJuegoBoard board;
    private PantallaJuegoController controller;

    private Socket mSocket;
    private android.app.Dialog dialogoEspera;
    private android.app.Dialog dialogoEsperandoPausa;
    private android.app.Dialog dialogoDesconexionRival;
    private android.os.CountDownTimer timerDesconexion;

    private final Map<String, UserShip> diccionarioFlota = new HashMap<>();
    private boolean diccionarioListo = false;
    private Object[] mensajeRetrasadoStartInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        Intent intent = getIntent();
        matchId = intent.getStringExtra("MATCH_ID");
        codigoSala = intent.getStringExtra("CODIGO_SALA");
        esHost = intent.getBooleanExtra("ES_HOST", false);

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        token = prefs.getString("token", "");
        myUserId = prefs.getString("userId", "");

        Log.d(TAG, "Iniciando PantallaJuego. matchId=" + matchId + " | userId=" + myUserId + " | esHost=" + esHost);

        ui = new PantallaJuegoUi(this);

        board = new PantallaJuegoBoard(
                this,
                casilla -> {
                    if (controller != null) controller.manejarToqueCasilla(casilla);
                }
        );

        controller = new PantallaJuegoController(this, ui, board);

        configurarBotonesBase();

        mSocket = SocketManager.getInstance().getSocket();

        if (GameStartCache.pendingStartInfo != null) {
            mensajeRetrasadoStartInfo = new Object[]{GameStartCache.pendingStartInfo};
            GameStartCache.pendingStartInfo = null;
            Log.d(TAG, "match:startInfo recuperado desde caché");
        }

        if (mSocket != null) {
            mSocket.on("match:startInfo", args -> runOnUiThread(() -> {
                if (gestor == null) {
                    mensajeRetrasadoStartInfo = args;
                    Log.d(TAG, "match:startInfo recibido antes de crear GestorJuego, se guarda temporalmente");
                } else {
                    Log.d(TAG, "match:startInfo ignorado en PantallaJuego porque ya lo gestiona GestorJuegoSocketBinder");
                }
            }));

            mSocket.on("match:ready", args -> runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    matchId = data.getString("matchId");
                    Log.d(TAG, "match:ready recibido -> matchId=" + matchId);

                    if (dialogoEspera != null) dialogoEspera.dismiss();
                    intentarArrancarPartida();
                } catch (Exception e) {
                    Log.e(TAG, "Fallo al leer match:ready", e);
                }
            }));
        } else {
            Log.e(TAG, "Socket nulo en PantallaJuego");
        }

        boolean esReconexion = getIntent().getBooleanExtra("esReconexion", false);
        String idReconexion = getIntent().getStringExtra("matchId");

        if (esReconexion && idReconexion != null) {
            matchId = idReconexion;
            Log.d(TAG, "Reconectando a partida activa: " + matchId);
            try {
                JSONObject payload = new JSONObject();
                payload.put("matchId", matchId);
                mSocket.emit("game:join", payload);
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (esHost && (matchId == null || matchId.isEmpty())) {
            mostrarPopUpEspera(codigoSala);
        }

        descargarDiccionarioFlota();
        intentarArrancarPartida();
    }

    private void configurarBotonesBase() {
        ui.btnPasarTurno.setOnClickListener(v -> {
            if (gestor != null) {
                gestor.terminarTurno();
            }
        });

        ui.btnPause.setOnClickListener(v -> {
            if (gestor == null) return;
            mostrarDialogoMiMenuPausa();
        });
    }

    private void descargarDiccionarioFlota() {
        ApiClient.getApiService()
                .obtenerInventarioBarcos("Bearer " + token)
                .enqueue(new Callback<List<UserShip>>() {
                    @Override
                    public void onResponse(Call<List<UserShip>> call, Response<List<UserShip>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (UserShip barco : response.body()) {
                                diccionarioFlota.put(barco.getId(), barco);
                            }
                            Log.d(TAG, "Inventario cargado. barcos=" + diccionarioFlota.size());
                        } else {
                            Log.e(TAG, "No se pudo cargar inventario. code=" + response.code());
                        }

                        diccionarioListo = true;
                        intentarArrancarPartida();
                    }

                    @Override
                    public void onFailure(Call<List<UserShip>> call, Throwable t) {
                        Log.e(TAG, "Error descargando diccionarioFlota", t);
                        diccionarioListo = true;
                        intentarArrancarPartida();
                    }
                });
    }

    private void intentarArrancarPartida() {
        if (diccionarioListo && matchId != null && !matchId.isEmpty() && gestor == null) {
            Log.d(TAG, "Condiciones cumplidas para arrancar partida");
            iniciarGestorJuego(matchId);
        }
    }

    private void iniciarGestorJuego(String mId) {
        this.matchId = mId;

        gestor = new GestorJuego(
                mSocket,
                matchId,
                myUserId,
                new GestorJuego.PartidaListener() {
                    @Override
                    public void onSnapshotCompleto() {
                        runOnUiThread(() -> {
                            controller.setGestor(gestor);
                            controller.setDiccionarioFlota(diccionarioFlota);
                            ui.actualizarTurno(gestor != null && gestor.isEsMiTurno(), PantallaJuego.this);
                            if (ui != null) {
                                ui.actualizarTurnoDisplay(gestor.numeroTurno, gestor.esMiTurno);
                            }
                            board.repaintFull(
                                    gestor,
                                    controller.getIdBarcoSeleccionado(),
                                    controller.getPosicionesRangoActual()
                            );

                            board.repaintFull(
                                    gestor,
                                    controller.getIdBarcoSeleccionado(),
                                    controller.getPosicionesRangoActual()
                            );

                            // 1. Buscamos el tablero
                            androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvBoard);

                            // 2. rv.post() asegura que el código de dentro SOLO se ejecute
                            // CUANDO Android haya terminado de calcular y colocar las 225 casillas
                            rv.post(() -> {
                                // 3. Le damos un pequeñísimo respiro a la gráfica (200ms) para
                                // cargar los colores de la niebla y los barcos por detrás.
                                rv.postDelayed(() -> {
                                    android.view.View telon = findViewById(R.id.layCargaTablero);
                                    if (telon != null && telon.getVisibility() == android.view.View.VISIBLE) {
                                        // Animamos la desaparición
                                        telon.animate()
                                                .alpha(0f)
                                                .setDuration(300)
                                                .withEndAction(() -> {
                                                    telon.setVisibility(android.view.View.GONE);
                                                    telon.setAlpha(1f); // Lo restauramos por si hay reconexiones
                                                })
                                                .start();
                                    }
                                }, 200);
                            });
                        });
                    }

                    @Override
                    public void onRecursosActualizados(int fuel, int ammo) {
                        runOnUiThread(() -> ui.actualizarRecursos(fuel, ammo));
                    }

                    @Override
                    public void onPartidaTerminada(String ganadorId, String razon) {
                        runOnUiThread(() -> {
                            if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) {
                                dialogoDesconexionRival.dismiss();
                            }
                            if (timerDesconexion != null) {
                                timerDesconexion.cancel();
                            }
                            mostrarDialogoFinPartida(ganadorId, razon);
                        });
                    }

                    @Override
                    public void onErrorJuego(String mensaje) {
                        runOnUiThread(() -> controller.mostrarError(mensaje));
                    }

                    @Override
                    public void onBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo) {
                        runOnUiThread(() ->
                                controller.actualizarCeldasBarcoMovido(
                                        shipId, oldX, oldY, newX, newY, orientation, tipo
                                )
                        );
                    }

                    @Override
                    public void onBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo) {
                        runOnUiThread(() ->
                                controller.actualizarCeldasBarcoRotado(
                                        shipId, x, y, oldOrientation, newOrientation, tipo
                                )
                        );
                    }

                    @Override
                    public void onVisionUpdateParcial(List<BarcoLogico> flotaAnterior, List<BarcoLogico> flotaNueva) {
                        runOnUiThread(() -> {
                            controller.setGestor(gestor);
                            controller.setDiccionarioFlota(diccionarioFlota);
                            ui.actualizarTurno(gestor != null && gestor.isEsMiTurno(), PantallaJuego.this);
                            board.repaintDiffFlotas(
                                    flotaAnterior,
                                    flotaNueva,
                                    gestor,
                                    controller.getIdBarcoSeleccionado(),
                                    controller.getPosicionesRangoActual()
                            );
                        });
                    }

                    @Override
                    public void onOponenteConexionCambio(boolean conectado, String mensaje) {
                        runOnUiThread(() -> {
                            if (!conectado) {
                                ui.setEstadoBloqueoRed(true);

                                if (dialogoDesconexionRival == null) {
                                    dialogoDesconexionRival = new android.app.Dialog(PantallaJuego.this);
                                    dialogoDesconexionRival.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                                    dialogoDesconexionRival.setContentView(R.layout.dialog_personalizado);

                                    if (dialogoDesconexionRival.getWindow() != null) {
                                        dialogoDesconexionRival.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                                        dialogoDesconexionRival.getWindow().setLayout(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                        );
                                    }
                                    dialogoDesconexionRival.setCancelable(false);

                                    android.widget.TextView tvTitulo = dialogoDesconexionRival.findViewById(R.id.tvDialogTitle);
                                    android.widget.Button btnAccion = dialogoDesconexionRival.findViewById(R.id.btnDialogAction);
                                    android.widget.Button btnCancelar = dialogoDesconexionRival.findViewById(R.id.btnDialogCancel);
                                    android.widget.ImageView ivIcono = dialogoDesconexionRival.findViewById(R.id.ivDialogIcon);

                                    tvTitulo.setText("CONEXIÓN PERDIDA");
                                    tvTitulo.setTextColor(android.graphics.Color.parseColor("#B71C1C"));
                                    ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
                                    ivIcono.setColorFilter(android.graphics.Color.parseColor("#B71C1C"));

                                    btnAccion.setVisibility(android.view.View.GONE);
                                    btnCancelar.setVisibility(android.view.View.GONE);
                                }

                                dialogoDesconexionRival.show();
                                AppNotifier.show(PantallaJuego.this, "Rival desconectado", AppNotifier.Type.ERROR);

                                if (timerDesconexion != null) {
                                    timerDesconexion.cancel();
                                }
                                timerDesconexion = new android.os.CountDownTimer(120000, 1000) {
                                    public void onTick(long millisUntilFinished) {
                                        long segundosTotales = millisUntilFinished / 1000;
                                        long minutos = segundosTotales / 60;
                                        long segundos = segundosTotales % 60;

                                        String tiempoFormateado = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutos, segundos);

                                        if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) {
                                            android.widget.TextView tvMensaje = dialogoDesconexionRival.findViewById(R.id.tvDialogMessage);
                                            if (tvMensaje != null) {
                                                tvMensaje.setText(mensaje + "\n\nTiempo restante: " + tiempoFormateado);
                                            }
                                        }
                                    }

                                    public void onFinish() {
                                        if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) {
                                            android.widget.TextView tvMensaje = dialogoDesconexionRival.findViewById(R.id.tvDialogMessage);
                                            if (tvMensaje != null) {
                                                tvMensaje.setText("El tiempo se ha agotado. Esperando la resolución del servidor...");
                                            }
                                        }
                                    }
                                }.start();

                            } else {
                                ui.setEstadoBloqueoRed(false);
                                if (timerDesconexion != null) {
                                    timerDesconexion.cancel();
                                }
                                if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) {
                                    dialogoDesconexionRival.dismiss();
                                }
                                AppNotifier.show(PantallaJuego.this, mensaje, AppNotifier.Type.SUCCESS);
                            }
                        });
                    }

                    @Override
                    public void onPausaSolicitada(String oponente) {
                        runOnUiThread(() -> {
                            mostrarDialogoOponentePidePausa(oponente);
                        });
                    }

                    @Override
                    public void onPartidaPausadaConfirmada(String mensaje) {
                        runOnUiThread(() -> {
                            if (dialogoEsperandoPausa != null && dialogoEsperandoPausa.isShowing()) {
                                dialogoEsperandoPausa.dismiss();
                            }

                            AppNotifier.show(PantallaJuego.this, mensaje, AppNotifier.Type.INFO);

                            new android.os.Handler().postDelayed(() -> {
                                Intent intent = new Intent(PantallaJuego.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 2000);
                        });
                    }

                    @Override
                    public void onPausaRechazada(String mensaje) {
                        runOnUiThread(() -> {
                            mostrarDialogoPausaRechazada();
                        });
                    }

                    @Override
                    public void onTurnoCambiado(int turno, boolean esMiTurno) {
                        Log.d("DEBUG_TURNO", "Gestor avisa a la Activity. ¿La interfaz UI está lista?: " + (ui != null));

                        if (ui != null) {
                            runOnUiThread(() -> {
                                ui.actualizarTurnoDisplay(turno, esMiTurno);
                                ui.actualizarTurno(esMiTurno, PantallaJuego.this);
                            });
                        } else {
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                if (ui != null) {
                                    runOnUiThread(() -> {
                                        ui.actualizarTurnoDisplay(turno, esMiTurno);
                                        ui.actualizarTurno(esMiTurno, PantallaJuego.this);
                                    });
                                }
                            }, 500);
                        }
                    }
                },
                diccionarioFlota
        );

        controller.setGestor(gestor);
        controller.setDiccionarioFlota(diccionarioFlota);
        controller.configurarBotones();

        if (mensajeRetrasadoStartInfo != null) {
            Log.d(TAG, "Procesando match:startInfo retrasado");
            gestor.procesarStartInfo(mensajeRetrasadoStartInfo);
            mensajeRetrasadoStartInfo = null;
        }
    }

    private void mostrarPopUpEspera(String codigo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Esperando rival");
        builder.setMessage("Código de sala: " + (codigo != null ? codigo : ""));
        builder.setCancelable(false);
        dialogoEspera = builder.create();
        dialogoEspera.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (timerDesconexion != null) timerDesconexion.cancel();
        if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) dialogoDesconexionRival.dismiss();
        if (dialogoEsperandoPausa != null && dialogoEsperandoPausa.isShowing()) dialogoEsperandoPausa.dismiss();
        if (dialogoEspera != null && dialogoEspera.isShowing()) dialogoEspera.dismiss();

        if (mSocket != null) {
            mSocket.off("match:startInfo");
            mSocket.off("match:ready");
        }

        if (gestor != null) {
            gestor.liberarListeners();
        }
    }

    // ------------------ POPUPS PERSONALIZADOS ------------------

    private void mostrarDialogoFinPartida(String ganadorId, String razon) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setCancelable(false);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnAccion = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        btnCancelar.setVisibility(android.view.View.GONE);
        btnAccion.setText("VOLVER A LA BASE");

        boolean heGanado = ganadorId != null && ganadorId.equals(myUserId);

        if (heGanado) {
            tvTitulo.setText("¡VICTORIA!");
            if ("abandonment".equals(razon)) {
                tvMensaje.setText("El enemigo ha huido cobardemente. ¡Has ganado por abandono de la flota rival!");
            } else {
                tvMensaje.setText("¡Has hundido la flota enemiga por completo! Eres el amo de los mares.");
            }
            ivIcono.setImageResource(android.R.drawable.btn_star_big_on);
            ivIcono.setColorFilter(android.graphics.Color.parseColor("#2E7D32"));
            tvTitulo.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            tvTitulo.setText("DERROTA");
            tvMensaje.setText("Tu flota ha sido aniquilada. Retirada táctica al astillero para reparaciones...");
            ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
            ivIcono.setColorFilter(android.graphics.Color.parseColor("#B71C1C"));
            tvTitulo.setTextColor(android.graphics.Color.parseColor("#B71C1C"));
        }

        btnAccion.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PantallaJuego.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    private void mostrarDialogoOponentePidePausa(String oponente) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setCancelable(false);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnAceptar = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnRechazar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("SOLICITUD DE TREGUA");
        tvMensaje.setText("El comandante rival (" + oponente + ") solicita pausar la batalla temporalmente. La partida se guardará.\n\n¿Aceptas la tregua?");

        ivIcono.setImageResource(android.R.drawable.ic_dialog_info);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#1976D2"));
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#1976D2"));

        btnRechazar.setVisibility(android.view.View.VISIBLE);
        btnRechazar.setText("DENEGAR");
        btnAceptar.setText("ACEPTAR TREGUA");

        btnRechazar.setOnClickListener(v -> {
            dialog.dismiss();
            if(gestor != null) gestor.responderPausa(false);
        });

        btnAceptar.setOnClickListener(v -> {
            dialog.dismiss();
            if(gestor != null) gestor.responderPausa(true);
        });

        dialog.show();
    }

    private void mostrarDialogoMiMenuPausa() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setCancelable(true);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnSolicitar = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnRendirse = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("MENÚ DE COMANDO");
        tvMensaje.setText("¿Qué orden deseas ejecutar, comandante?\n\nPuedes solicitar una tregua al rival para guardar la partida, o izar la bandera blanca y rendirte.");

        ivIcono.setImageResource(android.R.drawable.ic_menu_manage);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#5C3A21"));
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#5C3A21"));

        btnRendirse.setVisibility(android.view.View.VISIBLE);
        btnRendirse.setText("RENDIRSE");
        btnSolicitar.setText("PAUSAR");

        btnRendirse.setOnClickListener(v -> {
            dialog.dismiss();
            if(gestor != null) gestor.rendirse();
        });

        btnSolicitar.setOnClickListener(v -> {
            dialog.dismiss();
            if(gestor != null) {
                mostrarDialogoEsperandoPausa();
                gestor.solicitarPausa();
            }
        });

        dialog.show();
    }

    private void mostrarDialogoEsperandoPausa() {
        dialogoEsperandoPausa = new android.app.Dialog(this);
        dialogoEsperandoPausa.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialogoEsperandoPausa.setContentView(R.layout.dialog_personalizado);

        if (dialogoEsperandoPausa.getWindow() != null) {
            dialogoEsperandoPausa.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialogoEsperandoPausa.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialogoEsperandoPausa.setCancelable(false);

        android.widget.TextView tvTitulo = dialogoEsperandoPausa.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialogoEsperandoPausa.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnAccion = dialogoEsperandoPausa.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialogoEsperandoPausa.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialogoEsperandoPausa.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("TREGUA ENVIADA");
        tvMensaje.setText("Esperando la respuesta del comandante rival...");

        ivIcono.setImageResource(android.R.drawable.ic_popup_sync);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#5C3A21"));

        btnAccion.setVisibility(android.view.View.GONE);
        btnCancelar.setVisibility(android.view.View.GONE);

        dialogoEsperandoPausa.show();
    }

    private void mostrarDialogoPausaRechazada() {
        if (dialogoEsperandoPausa != null && dialogoEsperandoPausa.isShowing()) {
            dialogoEsperandoPausa.dismiss();
        }

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setCancelable(false);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnAccion = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("TREGUA DENEGADA");
        tvMensaje.setText("El enemigo ha rechazado tu solicitud de tregua. ¡La batalla continúa!");

        ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#B71C1C"));
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#B71C1C"));

        btnCancelar.setVisibility(android.view.View.GONE);
        btnAccion.setText("A LAS ARMAS");

        btnAccion.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}