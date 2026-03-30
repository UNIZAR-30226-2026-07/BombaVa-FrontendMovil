package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bombavafrontmovil.models.UserShip;
import com.example.bombavafrontmovil.network.ApiClient;
import com.example.bombavafrontmovil.network.SocketManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
    private final List<Casilla> matriz = new ArrayList<>();
    private BoardAdapter adapter;
    private String idBarcoSeleccionado = null;

    private View layNoSel, layMain, layMove, layAtk, panelInfoBarco;
    private TextView txtInfoTitulo, txtInfoGlobal, txtInfoCeldas, txtTurnoStatus;
    private Button btnPasarTurno;
    private ImageButton btnPause;
    private Button btnAtk1, btnAtk2, btnAtk3;
    private ProgressBar barFuel, barAmmo;
    private TextView txtFuel, txtAmmo;

    private int tipoAtaque = 0;
    private int rangoAtaqueActual = 0;
    private final LinkedHashSet<Integer> posicionesRangoActual = new LinkedHashSet<>();

    private Socket mSocket;
    private android.app.Dialog dialogoEspera;

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

        Log.d(TAG, "Iniciando PantallaJuego. MatchID: " + matchId + " | Mi UserID: " + myUserId);

        initViews();

        for (int i = 0; i < 225; i++) {
            matriz.add(new Casilla(i / 15, i % 15));
        }

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));
        adapter = new BoardAdapter(matriz, this::manejarToqueCasilla);
        rv.setAdapter(adapter);

        configurarBotones();

        mSocket = SocketManager.getInstance().getSocket();

        if (GameStartCache.pendingStartInfo != null) {
            mensajeRetrasadoStartInfo = new Object[]{GameStartCache.pendingStartInfo};
            GameStartCache.pendingStartInfo = null;
            Log.d(TAG, "match:startInfo recuperado desde caché");
        }

        mSocket.on("match:startInfo", args -> runOnUiThread(() -> {
            if (gestor == null) {
                mensajeRetrasadoStartInfo = args;
                Log.w(TAG, "match:startInfo recibido por socket mientras GestorJuego aún no existe");
            } else {
                gestor.procesarStartInfo(args);
            }
        }));

        mSocket.on("match:ready", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                matchId = data.getString("matchId");
                Log.d(TAG, "UUID de partida recibido del servidor: " + matchId);

                if (dialogoEspera != null) {
                    dialogoEspera.dismiss();
                }

                intentarArrancarPartida();
            } catch (Exception e) {
                Log.e(TAG, "Fallo al leer match:ready", e);
            }
        }));

        if (esHost && (matchId == null || matchId.isEmpty())) {
            mostrarPopUpEspera(codigoSala);
        }

        descargarDiccionarioFlota();
        intentarArrancarPartida();
    }

    private void initViews() {
        layNoSel = findViewById(R.id.txtNoSelection);
        layMain = findViewById(R.id.layoutMainActions);
        layMove = findViewById(R.id.layoutMoveActions);
        layAtk = findViewById(R.id.layoutAttackActions);

        panelInfoBarco = findViewById(R.id.panelInfoBarco);
        txtInfoTitulo = findViewById(R.id.txtInfoTitulo);
        txtInfoGlobal = findViewById(R.id.txtInfoGlobal);
        txtInfoCeldas = findViewById(R.id.txtInfoCeldas);

        txtTurnoStatus = findViewById(R.id.txtTurnoStatus);
        btnPasarTurno = findViewById(R.id.btnPasarTurno);
        btnPause = findViewById(R.id.btnPause);

        barFuel = findViewById(R.id.barFuel);
        txtFuel = findViewById(R.id.txtFuel);
        barAmmo = findViewById(R.id.barAmmo);
        txtAmmo = findViewById(R.id.txtAmmo);

        btnAtk1 = findViewById(R.id.btnAtk1);
        btnAtk2 = findViewById(R.id.btnAtk2);
        btnAtk3 = findViewById(R.id.btnAtk3);

        if (txtInfoCeldas != null) {
            txtInfoCeldas.setText("");
            txtInfoCeldas.setVisibility(View.GONE);
        }

        mostrar(layNoSel);
    }

    private void configurarBotones() {
        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));

        findViewById(R.id.btnMainAttack).setOnClickListener(v -> {
            if (idBarcoSeleccionado == null) {
                Toast.makeText(this, "Selecciona antes un barco aliado", Toast.LENGTH_SHORT).show();
                return;
            }
            actualizarBotonesArmas(idBarcoSeleccionado);
            mostrar(layAtk);
        });

        btnPasarTurno.setOnClickListener(v -> {
            if (gestor != null) gestor.terminarTurno();
        });

        btnPause.setOnClickListener(v -> {
            if (gestor == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Pausa")
                    .setNegativeButton("Rendirse", (d, w) -> gestor.rendirse())
                    .show();
        });

        View.OnClickListener movListener = v -> {
            if (idBarcoSeleccionado == null || gestor == null) return;
            if (!gestor.isEsMiTurno()) {
                Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                return;
            }

            BarcoLogico b = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
            if (b == null) return;

            cancelarModoAtaque();

            String dir;
            int id = v.getId();

            if (id == R.id.btnForward) {
                dir = direccionAdelanteVisual(b.orientation);
            } else {
                dir = direccionAtrasVisual(b.orientation);
            }

            gestor.moverBarco(idBarcoSeleccionado, dir);
            mostrar(layMain);
        };

        findViewById(R.id.btnForward).setOnClickListener(movListener);
        findViewById(R.id.btnBackward).setOnClickListener(movListener);

        findViewById(R.id.btnRotateL).setOnClickListener(v -> {
            if (idBarcoSeleccionado == null || gestor == null) return;
            if (!gestor.isEsMiTurno()) {
                Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                return;
            }

            cancelarModoAtaque();
            gestor.rotarBarco(idBarcoSeleccionado, -90);
            mostrar(layMain);
        });

        findViewById(R.id.btnRotateR).setOnClickListener(v -> {
            if (idBarcoSeleccionado == null || gestor == null) return;
            if (!gestor.isEsMiTurno()) {
                Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                return;
            }

            cancelarModoAtaque();
            gestor.rotarBarco(idBarcoSeleccionado, 90);
            mostrar(layMain);
        });

        btnAtk1.setOnClickListener(v -> {
            if (idBarcoSeleccionado == null) {
                Toast.makeText(this, "Selecciona antes un barco aliado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gestor == null || !gestor.isEsMiTurno()) {
                Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                return;
            }

            tipoAtaque = 1;
            rangoAtaqueActual = 4;
            panelInfoBarco.setVisibility(View.GONE);
            actualizarRangoAtaqueVisual();
            Toast.makeText(this, "Selecciona la casilla objetivo para disparar", Toast.LENGTH_SHORT).show();
        });

        btnAtk2.setOnClickListener(v -> {
            if (gestor != null && idBarcoSeleccionado != null) {
                if (!gestor.isEsMiTurno()) {
                    Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                    return;
                }
                cancelarModoAtaque();
                gestor.lanzarTorpedo(idBarcoSeleccionado);
            }
            mostrar(layMain);
        });

        btnAtk3.setOnClickListener(v -> {
            if (idBarcoSeleccionado == null) {
                Toast.makeText(this, "Selecciona antes un barco aliado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gestor == null || !gestor.isEsMiTurno()) {
                Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                return;
            }

            tipoAtaque = 2;
            rangoAtaqueActual = 1;
            panelInfoBarco.setVisibility(View.GONE);
            actualizarRangoAtaqueVisual();
            Toast.makeText(this, "Selecciona la casilla donde quieres colocar la mina", Toast.LENGTH_SHORT).show();
        });

        View btnCloseInfo = findViewById(R.id.btnCloseInfo);
        if (btnCloseInfo != null) {
            btnCloseInfo.setOnClickListener(v -> {
                panelInfoBarco.setVisibility(View.GONE);
                if (txtInfoCeldas != null) {
                    txtInfoCeldas.setText("");
                    txtInfoCeldas.setVisibility(View.GONE);
                }
            });
        }

        View btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                if (idBarcoSeleccionado == null || gestor == null) return;

                BarcoLogico barco = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
                if (barco != null && barco.esAliado) {
                    mostrarInfoBarco(barco);
                }
            });
        }
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
                        }
                        diccionarioListo = true;
                        intentarArrancarPartida();
                    }

                    @Override
                    public void onFailure(Call<List<UserShip>> call, Throwable t) {
                        diccionarioListo = true;
                        intentarArrancarPartida();
                    }
                });
    }

    private void intentarArrancarPartida() {
        if (diccionarioListo && matchId != null && !matchId.isEmpty() && gestor == null) {
            Log.d(TAG, "Diccionario y MatchID listos, arrancando Gestor de Juego");
            iniciarGestorJuego(matchId);
        }
    }

    private void iniciarGestorJuego(String mId) {
        this.matchId = mId;

        gestor = new GestorJuego(
                mSocket,
                matchId,
                myUserId,
                diccionarioFlota,
                new GestorJuego.PartidaListener() {
                    @Override
                    public void onSnapshotCompleto() {
                        runOnUiThread(() -> {
                            actualizarInterfazTurno();
                            actualizarMatrizVisualCompleta();
                        });
                    }

                    @Override
                    public void onRecursosActualizados(int fuel, int ammo) {
                        runOnUiThread(() -> {
                            if (fuel >= 0) {
                                barFuel.setProgress(fuel);
                                txtFuel.setText(String.valueOf(fuel));
                            }
                            if (ammo >= 0) {
                                barAmmo.setProgress(ammo);
                                txtAmmo.setText(String.valueOf(ammo));
                            }
                        });
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
                        runOnUiThread(() ->
                                Toast.makeText(PantallaJuego.this, mensaje, Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo) {
                        runOnUiThread(() -> actualizarCeldasBarcoMovido(shipId, oldX, oldY, newX, newY, orientation, tipo));
                    }

                    @Override
                    public void onBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo) {
                        runOnUiThread(() -> actualizarCeldasBarcoRotado(shipId, x, y, oldOrientation, newOrientation, tipo));
                    }
                }
        );

        if (mensajeRetrasadoStartInfo != null) {
            Log.d(TAG, "Inyectando match:startInfo retrasado...");
            gestor.procesarStartInfo(mensajeRetrasadoStartInfo);
            mensajeRetrasadoStartInfo = null;
        }
    }

    private void actualizarBotonesArmas(String idBarco) {
        btnAtk1.setVisibility(View.GONE);
        btnAtk2.setVisibility(View.GONE);
        btnAtk3.setVisibility(View.GONE);

        UserShip ship = diccionarioFlota.get(idBarco);
        if (ship == null || ship.getWeaponTemplates() == null) return;

        for (UserShip.WeaponItem w : ship.getWeaponTemplates()) {
            if ("cannon-base".equals(w.slug)) {
                btnAtk1.setVisibility(View.VISIBLE);
                btnAtk1.setText("CAÑÓN");
            } else if ("torpedo-v1".equals(w.slug)) {
                btnAtk2.setVisibility(View.VISIBLE);
                btnAtk2.setText("TORPEDO");
            } else if ("mine-v1".equals(w.slug)) {
                btnAtk3.setVisibility(View.VISIBLE);
                btnAtk3.setText("MINA");
            }
        }
    }

    private void manejarToqueCasilla(Casilla c) {
        if (gestor == null) return;

        BarcoLogico barco = gestor.obtenerBarcoEn(c.getFila(), c.getColumna());

        if (barco != null && barco.esAliado) {
            String anterior = idBarcoSeleccionado;
            idBarcoSeleccionado = barco.id;
            cancelarModoAtaque();
            actualizarBotonesArmas(barco.id);
            panelInfoBarco.setVisibility(View.GONE);
            mostrar(layMain);
            actualizarSeleccionBarco(anterior, idBarcoSeleccionado);
            return;
        }

        if (tipoAtaque == 1 && idBarcoSeleccionado != null && gestor.isEsMiTurno()) {
            if (!c.isEnRangoAtaque()) {
                Toast.makeText(this, "Esa casilla está fuera de rango", Toast.LENGTH_SHORT).show();
                return;
            }
            gestor.dispararCannon(idBarcoSeleccionado, c.getFila(), c.getColumna());
            cancelarModoAtaque();
            mostrar(layMain);
            return;
        }

        if (tipoAtaque == 2 && idBarcoSeleccionado != null && gestor.isEsMiTurno()) {
            if (!c.isEnRangoAtaque()) {
                Toast.makeText(this, "Esa casilla está fuera de rango", Toast.LENGTH_SHORT).show();
                return;
            }
            gestor.colocarMina(idBarcoSeleccionado, c.getFila(), c.getColumna());
            cancelarModoAtaque();
            mostrar(layMain);
            return;
        }

        String anterior = idBarcoSeleccionado;
        idBarcoSeleccionado = null;
        cancelarModoAtaque();
        panelInfoBarco.setVisibility(View.GONE);
        mostrar(layNoSel);
        actualizarSeleccionBarco(anterior, null);
    }

    private void cancelarModoAtaque() {
        tipoAtaque = 0;
        rangoAtaqueActual = 0;
        limpiarRangoAtaqueVisual();
    }

    private void actualizarSeleccionBarco(String idAnterior, String idNuevo) {
        if (gestor == null) return;

        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();

        for (BarcoLogico b : gestor.getFlota()) {
            boolean afecta =
                    (idAnterior != null && idAnterior.equals(b.id)) ||
                            (idNuevo != null && idNuevo.equals(b.id));

            if (!afecta) continue;

            posiciones.addAll(posicionesVisualesDeBarco(b));
        }

        for (Integer pos : posiciones) {
            Casilla casilla = matriz.get(pos);
            String shipId = casilla.getIdBarcoStr();
            casilla.setSeleccionado(shipId != null && shipId.equals(idNuevo) && casilla.isEsAliado());
        }

        repintarPosiciones(posiciones);
    }

    private void actualizarRangoAtaqueVisual() {
        LinkedHashSet<Integer> anteriores = new LinkedHashSet<>(posicionesRangoActual);
        posicionesRangoActual.clear();

        if (gestor == null || idBarcoSeleccionado == null || rangoAtaqueActual <= 0) {
            repintarPosiciones(anteriores);
            return;
        }

        BarcoLogico barco = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
        if (barco == null) {
            repintarPosiciones(anteriores);
            return;
        }

        int origenX = barco.x;
        int origenY = barco.y;

        for (Casilla c : matriz) {
            int x = c.getColumna();
            int yLogica = 14 - c.getFila();

            int dx = Math.abs(x - origenX);
            int dy = Math.abs(yLogica - origenY);

            boolean enRango = Math.max(dx, dy) <= rangoAtaqueActual;
            if (enRango) {
                posicionesRangoActual.add(c.getFila() * 15 + c.getColumna());
            }
        }

        LinkedHashSet<Integer> aRepintar = new LinkedHashSet<>(anteriores);
        aRepintar.addAll(posicionesRangoActual);
        repintarPosiciones(aRepintar);
    }

    private void limpiarRangoAtaqueVisual() {
        if (posicionesRangoActual.isEmpty()) return;
        LinkedHashSet<Integer> anteriores = new LinkedHashSet<>(posicionesRangoActual);
        posicionesRangoActual.clear();
        repintarPosiciones(anteriores);
    }

    private void actualizarCeldasBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        posiciones.addAll(posicionesVisualesPara(oldX, oldY, orientation, tipo));
        posiciones.addAll(posicionesVisualesPara(newX, newY, orientation, tipo));
        posiciones.addAll(posicionesRangoActual);

        repintarPosiciones(posiciones);

        if (shipId.equals(idBarcoSeleccionado) && tipoAtaque != 0) {
            actualizarRangoAtaqueVisual();
        }
    }

    private void actualizarCeldasBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        posiciones.addAll(posicionesVisualesPara(x, y, oldOrientation, tipo));
        posiciones.addAll(posicionesVisualesPara(x, y, newOrientation, tipo));
        posiciones.addAll(posicionesRangoActual);

        repintarPosiciones(posiciones);

        if (shipId.equals(idBarcoSeleccionado) && tipoAtaque != 0) {
            actualizarRangoAtaqueVisual();
        }
    }

    private LinkedHashSet<Integer> posicionesVisualesDeBarco(BarcoLogico barco) {
        return posicionesVisualesPara(barco.x, barco.y, barco.orientation, barco.tipo);
    }

    private LinkedHashSet<Integer> posicionesVisualesPara(int x, int y, String orientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        List<int[]> celdas = BarcoLogico.getCeldasPara(x, y, orientation, tipo);

        for (int[] celda : celdas) {
            int filaVisual = 14 - celda[0];
            int col = celda[1];
            if (filaVisual < 0 || filaVisual >= 15 || col < 0 || col >= 15) continue;
            posiciones.add(filaVisual * 15 + col);
        }

        return posiciones;
    }

    private void repintarPosiciones(LinkedHashSet<Integer> posiciones) {
        if (posiciones.isEmpty()) return;

        for (Integer pos : posiciones) {
            Casilla c = matriz.get(pos);
            c.resetVisual();
        }

        if (gestor != null) {
            for (BarcoLogico b : gestor.getFlota()) {
                boolean esSel = (idBarcoSeleccionado != null && idBarcoSeleccionado.equals(b.id) && b.esAliado);
                int dir = orientacionADireccionVisual(b.orientation);

                for (int[] celdaBarco : b.getCeldas()) {
                    int filaVisual = 14 - celdaBarco[0];
                    int col = celdaBarco[1];

                    if (filaVisual < 0 || filaVisual >= 15 || col < 0 || col >= 15) continue;

                    int pos = filaVisual * 15 + col;
                    if (!posiciones.contains(pos)) continue;

                    Casilla cas = matriz.get(pos);
                    cas.setTieneBarco(true);
                    cas.setIdBarcoStr(b.id);
                    cas.setEsAliado(b.esAliado);
                    cas.setTipoBarco(b.tipo);
                    cas.setDireccion(dir);
                    cas.setEsProa(esProaVisual(celdaBarco[2] == 1, b.orientation, b.tipo));
                    cas.setIndiceEnBarco(indiceVisual(celdaBarco[3], b.tipo, b.orientation));
                    cas.setSlug(b.slug);
                    cas.setVidaActual(b.hpActual);
                    cas.setVidaMax(b.hpMax);
                    cas.setSeleccionado(esSel);
                }
            }
        }

        for (Integer pos : posiciones) {
            Casilla c = matriz.get(pos);
            c.setEnRangoAtaque(posicionesRangoActual.contains(pos));
        }

        for (Integer pos : posiciones) {
            adapter.notifyItemChanged(pos);
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

    private void actualizarInterfazTurno() {
        if (gestor != null && gestor.isEsMiTurno()) {
            txtTurnoStatus.setText("TU TURNO");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            btnPasarTurno.setEnabled(true);
            btnPasarTurno.setAlpha(1.0f);
        } else {
            txtTurnoStatus.setText("TURNO RIVAL");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            btnPasarTurno.setEnabled(false);
            btnPasarTurno.setAlpha(0.5f);
            mostrar(layNoSel);
        }
    }

    private String direccionAdelanteVisual(String orientationLogica) {
        if ("N".equals(orientationLogica)) return "S";
        if ("S".equals(orientationLogica)) return "N";
        if ("E".equals(orientationLogica)) return "E";
        if ("W".equals(orientationLogica)) return "W";
        return orientationLogica;
    }

    private String direccionAtrasVisual(String orientationLogica) {
        if ("N".equals(orientationLogica)) return "N";
        if ("S".equals(orientationLogica)) return "S";
        if ("E".equals(orientationLogica)) return "W";
        if ("W".equals(orientationLogica)) return "E";
        return orientationLogica;
    }

    private int orientacionADireccionVisual(String orientation) {
        if ("N".equals(orientation)) return 0;
        if ("E".equals(orientation)) return 1;
        if ("S".equals(orientation)) return 2;
        if ("W".equals(orientation)) return 3;
        return 0;
    }

    private boolean necesitaInvertirExtremosVisuales(String orientation) {
        return "N".equals(orientation) || "S".equals(orientation);
    }

    private int indiceVisual(int indiceLogico, int tipo, String orientation) {
        if (necesitaInvertirExtremosVisuales(orientation)) {
            return (tipo - 1) - indiceLogico;
        }
        return indiceLogico;
    }

    private boolean esProaVisual(boolean esProaLogica, String orientation, int tipo) {
        if (tipo <= 1) return esProaLogica;
        if (necesitaInvertirExtremosVisuales(orientation)) {
            return !esProaLogica;
        }
        return esProaLogica;
    }

    private void actualizarMatrizVisualCompleta() {
        if (gestor == null) return;

        List<Casilla> snapshotAnterior = new ArrayList<>(matriz.size());
        for (Casilla c : matriz) {
            snapshotAnterior.add(c.clonar());
        }

        for (Casilla c : matriz) {
            c.resetVisual();
        }

        for (BarcoLogico b : gestor.getFlota()) {
            boolean esSel = (idBarcoSeleccionado != null && idBarcoSeleccionado.equals(b.id) && b.esAliado);
            int dir = orientacionADireccionVisual(b.orientation);

            for (int[] celdaBarco : b.getCeldas()) {
                int filaLogica = celdaBarco[0];
                int col = celdaBarco[1];
                boolean esProa = esProaVisual(celdaBarco[2] == 1, b.orientation, b.tipo);
                int indice = indiceVisual(celdaBarco[3], b.tipo, b.orientation);

                int filaVisual = 14 - filaLogica;

                if (filaVisual < 0 || filaVisual >= 15 || col < 0 || col >= 15) continue;

                Casilla cas = matriz.get(filaVisual * 15 + col);
                cas.setTieneBarco(true);
                cas.setIdBarcoStr(b.id);
                cas.setEsAliado(b.esAliado);
                cas.setTipoBarco(b.tipo);
                cas.setDireccion(dir);
                cas.setEsProa(esProa);
                cas.setIndiceEnBarco(indice);
                cas.setSlug(b.slug);
                cas.setVidaActual(b.hpActual);
                cas.setVidaMax(b.hpMax);
                cas.setSeleccionado(esSel);
            }
        }

        for (Integer pos : posicionesRangoActual) {
            if (pos >= 0 && pos < matriz.size()) {
                matriz.get(pos).setEnRangoAtaque(true);
            }
        }

        for (int i = 0; i < matriz.size(); i++) {
            if (!matriz.get(i).equivaleA(snapshotAnterior.get(i))) {
                adapter.notifyItemChanged(i);
            }
        }
    }

    private void mostrarInfoBarco(BarcoLogico barco) {
        if (txtInfoTitulo != null) {
            txtInfoTitulo.setText("Barco aliado");
        }

        String nombreTipo = "Barco";
        if (barco.tipo == 1) nombreTipo = "Corbeta";
        else if (barco.tipo == 3) nombreTipo = "Fragata";
        else if (barco.tipo == 5) nombreTipo = "Acorazado";

        int vision = 0;
        if (barco.tipo == 1) vision = 4;
        else if (barco.tipo == 3) vision = 3;
        else if (barco.tipo == 5) vision = 2;

        if (txtInfoGlobal != null) {
            txtInfoGlobal.setText(
                    "Tipo: " + nombreTipo +
                            "\nVida: " + barco.hpActual + " / " + barco.hpMax +
                            "\nOrientación: " + barco.orientation +
                            "\nTamaño: " + barco.tipo + " casilla(s)" +
                            "\nVisión: " + vision
            );
        }

        if (txtInfoCeldas != null) {
            txtInfoCeldas.setText("");
            txtInfoCeldas.setVisibility(View.GONE);
        }

        if (panelInfoBarco != null) {
            panelInfoBarco.setVisibility(View.VISIBLE);
        }
    }

    private void mostrar(View layout) {
        if (layNoSel != null) layNoSel.setVisibility(View.GONE);
        if (layMain != null) layMain.setVisibility(View.GONE);
        if (layMove != null) layMove.setVisibility(View.GONE);
        if (layAtk != null) layAtk.setVisibility(View.GONE);

        if (layout != null) layout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSocket != null) {
            mSocket.off("match:startInfo");
            mSocket.off("match:ready");
        }

        if (gestor != null) {
            gestor.liberarListeners();
        }
    }
}