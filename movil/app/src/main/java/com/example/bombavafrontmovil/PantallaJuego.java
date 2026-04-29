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
    private AlertDialog dialogoDesconexionRival;
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
        String idReconexion = getIntent().getStringExtra("matchId"); // Usamos una variable temporal

        if (esReconexion && idReconexion != null) {
            matchId = idReconexion; // ¡Solo lo sobrescribimos si de verdad estamos reconectando!
            Log.d(TAG, "Reconectando a partida activa: " + matchId);
            // Emitimos el join directamente
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

            new AlertDialog.Builder(this)
                    .setTitle("Pausa")
                    .setNegativeButton("Rendirse", (d, w) -> gestor.rendirse())
                    .show();
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
                new GestorJuego.PartidaListener() { // <--- EL LISTENER VA ANTES
                    @Override
                    public void onSnapshotCompleto() {
                        runOnUiThread(() -> {
                            controller.setGestor(gestor);
                            controller.setDiccionarioFlota(diccionarioFlota);
                            ui.actualizarTurno(gestor != null && gestor.isEsMiTurno(), PantallaJuego.this);
                            board.repaintFull(
                                    gestor,
                                    controller.getIdBarcoSeleccionado(),
                                    controller.getPosicionesRangoActual()
                            );
                        });
                    }

                    @Override
                    public void onRecursosActualizados(int fuel, int ammo) {
                        runOnUiThread(() -> ui.actualizarRecursos(fuel, ammo));
                    }

                    @Override
                    public void onPartidaTerminada(String ganadorId, String razon) {
                        runOnUiThread(() -> new AlertDialog.Builder(PantallaJuego.this)
                                .setTitle("Fin de Partida")
                                .setMessage(ganadorId != null && ganadorId.equals(myUserId) ? "¡Ganaste!" : "Perdiste...")
                                .setCancelable(false)
                                .setPositiveButton("Salir", (d, w) -> finish())
                                .show());
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
                                // 1. Bloqueamos la interfaz
                                ui.setEstadoBloqueoRed(true);

                                // 2. Si no existe el diálogo, lo creamos
                                if (dialogoDesconexionRival == null) {
                                    dialogoDesconexionRival = new AlertDialog.Builder(PantallaJuego.this)
                                            .setTitle("Conexión perdida")
                                            .setMessage(mensaje)
                                            .setCancelable(false) // No se puede cerrar tocando fuera
                                            .create();
                                }
                                // Mostramos el diálogo
                                dialogoDesconexionRival.show();
                                AppNotifier.show(PantallaJuego.this, "Rival desconectado", AppNotifier.Type.ERROR);

                                // 3. Iniciamos el contador de 2 minutos (120,000 milisegundos)
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
                                            dialogoDesconexionRival.setMessage(mensaje + "\n\nTiempo restante: " + tiempoFormateado);
                                        }
                                    }

                                    public void onFinish() {
                                        if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) {
                                            dialogoDesconexionRival.setMessage("El tiempo se ha agotado. Esperando la resolución del servidor...");
                                        }
                                    }
                                }.start();

                            } else {
                                // RIVAL SE HA RECONECTADO

                                // 1. Desbloqueamos la interfaz
                                ui.setEstadoBloqueoRed(false);

                                // 2. Cancelamos el contador si estaba corriendo
                                if (timerDesconexion != null) {
                                    timerDesconexion.cancel();
                                }

                                // 3. Ocultamos el diálogo
                                if (dialogoDesconexionRival != null && dialogoDesconexionRival.isShowing()) {
                                    dialogoDesconexionRival.dismiss();
                                }

                                AppNotifier.show(PantallaJuego.this, mensaje, AppNotifier.Type.SUCCESS);
                            }
                        });
                    }

                    public void onPausaSolicitada(String oponente) {
                        runOnUiThread(() -> {
                            // CORREGIDO: Usamos PantallaJuego.this
                            new AlertDialog.Builder(PantallaJuego.this)
                                    .setTitle("Solicitud de Pausa")
                                    .setMessage(oponente + " quiere pausar la partida. ¿Aceptas?")
                                    .setPositiveButton("ACEPTAR", (d, w) -> gestor.responderPausa(true))
                                    .setNegativeButton("RECHAZAR", (d, w) -> gestor.responderPausa(false))
                                    .setCancelable(false)
                                    .show();
                        });
                    }

                    @Override
                    public void onPartidaPausadaConfirmada(String mensaje) {
                        runOnUiThread(() -> {
                            // CORREGIDO: Usamos PantallaJuego.this
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
                            // CORREGIDO: Usamos PantallaJuego.this
                            new AlertDialog.Builder(PantallaJuego.this)
                                    .setMessage(mensaje)
                                    .setPositiveButton("ENTENDIDO", null)
                                    .show();
                        });
                    }

                    // Y en el click del botón de pausa (normalmente en el Controller o aquí)
                    public void alPulsarBotonPausa() {
                        dialogoEspera = new AlertDialog.Builder(PantallaJuego.this)
                                .setMessage("Enviando solicitud de pausa...")
                                .setCancelable(false)
                                .show();
                        gestor.solicitarPausa();
                    }
                },
                diccionarioFlota // <--- EL MAPA VA AL FINAL
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

        // Parar el timer si salimos de la pantalla
        if (timerDesconexion != null) {
            timerDesconexion.cancel();
        }

        if (mSocket != null) {
            mSocket.off("match:startInfo");
            mSocket.off("match:ready");
        }

        if (gestor != null) {
            gestor.liberarListeners();
        }
    }
}