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
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PantallaJuego extends AppCompatActivity {

    private static final String TAG = "DEBUG_BOMBA";
    private String token, matchId, myUserId, codigoSala;
    private boolean esHost;

    private GestorJuego gestor;
    private List<Casilla> matriz = new ArrayList<>();
    private BoardAdapter adapter;
    private String idBarcoSeleccionado = null;

    private View layNoSel, layMain, layMove, layAtk, panelInfoBarco;
    private TextView txtInfoTitulo, txtInfoCeldas, txtTurnoStatus;
    private Button btnPasarTurno;
    private ImageButton btnPause;
    private Button btnAtk1, btnAtk2, btnAtk3;
    private ProgressBar barFuel, barAmmo;
    private TextView txtFuel, txtAmmo;
    private int tipoAtaque = 0;

    private Socket mSocket;
    private android.app.Dialog dialogoEspera;

    private Map<String, UserShip> diccionarioFlota = new HashMap<>();
    private boolean diccionarioListo = false;

    // 🔥 NUEVO: Trampa para evitar desincronización
    private Object[] mensajeRetrasadoStartInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        Intent intent = getIntent();
        matchId = intent.getStringExtra("MATCH_ID");
        codigoSala = intent.getStringExtra("CODIGO_SALA");
        esHost = intent.getBooleanExtra("ES_HOST", false);

        if (matchId == null && codigoSala != null) {
            matchId = codigoSala;
        }

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        token = prefs.getString("token", "");
        myUserId = prefs.getString("userId", "");

        Log.d(TAG, "Iniciando PantallaJuego. MatchID: " + matchId + " | Mi UserID: " + myUserId);

        initViews();

        for (int i = 0; i < 225; i++) matriz.add(new Casilla(i / 15, i % 15));

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));
        adapter = new BoardAdapter(matriz, c -> manejarToqueCasilla(c));
        rv.setAdapter(adapter);

        configurarBotones();

        mSocket = SocketManager.getInstance().getSocket();

        // 🔥 NUEVO: Atrapamos el evento si el servidor es más rápido que nuestra descarga
        mSocket.on("match:startInfo", args -> {
            if (gestor == null) {
                mensajeRetrasadoStartInfo = args;
                Log.w(TAG, "¡Mensaje atrapado en la red! Esperando al Gestor...");
            }
        });

        if (esHost) {
            mostrarPopUpEspera(codigoSala);
        }

        configurarEscuchaRival();
        descargarDiccionarioFlota();
    }

    private void intentarArrancarPartida() {
        if (diccionarioListo && matchId != null && !matchId.isEmpty() && gestor == null) {
            Log.d(TAG, "Todo ok, arrancando gestor");
            iniciarGestorJuego(matchId);
        }
    }

    private void configurarEscuchaRival() {
        mSocket.on("match:ready", (Object... args) -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    matchId = data.getString("matchId");
                    if (dialogoEspera != null) dialogoEspera.dismiss();
                    intentarArrancarPartida();
                } catch (Exception e) { Log.e(TAG, "Fallo al leer match:ready", e); }
            });
        });
    }

    private void descargarDiccionarioFlota() {
        ApiClient.getApiService().obtenerInventarioBarcos("Bearer " + token).enqueue(new Callback<List<UserShip>>() {
            @Override
            public void onResponse(Call<List<UserShip>> call, Response<List<UserShip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (UserShip barco : response.body()) {
                        diccionarioFlota.put(barco.getId(), barco);
                    }
                    Log.d(TAG, "Barcos descargados ok");
                } else {
                    Log.e(TAG, "Error api barcos: " + response.code());
                }
                diccionarioListo = true;
                intentarArrancarPartida();
            }

            @Override
            public void onFailure(Call<List<UserShip>> call, Throwable t) {
                Log.e(TAG, "Fallo de red al bajar barcos: " + t.getMessage());
                diccionarioListo = true;
                intentarArrancarPartida();
            }
        });
    }

    private void iniciarGestorJuego(String mId) {
        this.matchId = mId;
        gestor = new GestorJuego(mSocket, matchId, myUserId, diccionarioFlota, new GestorJuego.PartidaListener() {
            @Override
            public void onActualizarTablero() {
                runOnUiThread(() -> { actualizarInterfazTurno(); actualizarMatrizVisual(); });
            }
            @Override
            public void onRecursosActualizados(int fuel, int ammo) {
                runOnUiThread(() -> {
                    if (fuel >= 0) { barFuel.setProgress(fuel); txtFuel.setText(String.valueOf(fuel)); }
                    if (ammo >= 0) { barAmmo.setProgress(ammo); txtAmmo.setText(String.valueOf(ammo)); }
                });
            }
            @Override
            public void onPartidaTerminada(String ganadorId, String razon) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(PantallaJuego.this)
                            .setTitle("Fin de Partida")
                            .setMessage(ganadorId.equals(myUserId) ? "¡Ganaste!" : "Perdiste...")
                            .setCancelable(false)
                            .setPositiveButton("Salir", (d, w) -> finish()).show();
                });
            }
        });

        // 🔥 NUEVO: Si atrapamos un mensaje antes, lo forzamos ahora en el gestor (si tu gestor tiene un método para procesarlo, lo ideal es llamarlo aquí).
    }

    private void actualizarBotonesArmas(String idBarco) {
        btnAtk1.setVisibility(View.GONE);
        btnAtk2.setVisibility(View.GONE);
        btnAtk3.setVisibility(View.GONE);

        if (diccionarioFlota != null && diccionarioFlota.containsKey(idBarco)) {
            UserShip ship = diccionarioFlota.get(idBarco);
            if (ship != null && ship.getWeaponTemplates() != null) {
                for (UserShip.WeaponItem w : ship.getWeaponTemplates()) {
                    if (w.slug.equals("cannon-base")) {
                        btnAtk1.setVisibility(View.VISIBLE);
                        btnAtk1.setText("CAÑÓN");
                    } else if (w.slug.equals("torpedo-v1")) {
                        btnAtk2.setVisibility(View.VISIBLE);
                        btnAtk2.setText("TORPEDO");
                    } else if (w.slug.equals("mine-v1")) {
                        btnAtk3.setVisibility(View.VISIBLE);
                        btnAtk3.setText("MINA");
                    }
                }
            }
        }
    }

    private void manejarToqueCasilla(Casilla c) {
        if (gestor == null || !gestor.isEsMiTurno()) {
            Toast.makeText(this, "Espera a tu turno", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tipoAtaque > 0) {
            if (idBarcoSeleccionado != null) {
                if (tipoAtaque == 1) gestor.atacarCanon(idBarcoSeleccionado, c.getColumna(), c.getFila());
                else if (tipoAtaque == 2) gestor.ponerMina(idBarcoSeleccionado, c.getColumna(), c.getFila());
                tipoAtaque = 0;
                mostrar(layMain);
            }
        } else {
            if (c.isTieneBarco() && c.isEsAliado()) {
                idBarcoSeleccionado = c.getIdBarcoStr();
                actualizarBotonesArmas(idBarcoSeleccionado);
                mostrar(layMain);
            } else {
                idBarcoSeleccionado = null;
                mostrar(layNoSel);
            }
            actualizarMatrizVisual();
        }
    }

    private void mostrarPopUpEspera(String codigo) {
        dialogoEspera = new android.app.Dialog(this);
        dialogoEspera.setContentView(R.layout.dialog_codigo_partida);
        dialogoEspera.setCancelable(false);
        TextView tv = dialogoEspera.findViewById(R.id.tv_codigo_generado);
        if (tv != null) tv.setText(codigo);
        dialogoEspera.findViewById(R.id.btn_cerrar_popup).setOnClickListener(v -> {
            dialogoEspera.dismiss();
            finish();
        });
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

    // 🔥 ACTUALIZADO: Este es el motor que traduce los barcos a casillas dibujables
    private void actualizarMatrizVisual() {
        if (gestor == null) return;

        for (Casilla c : matriz) {
            c.setTieneBarco(false);
            c.setSeleccionado(false);
            c.setEsProa(false);
            c.setIndiceEnBarco(0);
            c.setTipoBarco(0);
            c.setDireccion(0);
        }

        for (BarcoLogico b : gestor.getFlota()) {
            boolean esSel = (idBarcoSeleccionado != null && idBarcoSeleccionado.equals(b.id));

            int dir = 0;
            if ("E".equals(b.orientation)) dir = 1;
            else if ("S".equals(b.orientation)) dir = 2;
            else if ("W".equals(b.orientation)) dir = 3;

            for (int[] celda : b.getCeldas()) {
                int idx = celda[0] * 15 + celda[1];
                if (idx >= 0 && idx < 225) {
                    Casilla c = matriz.get(idx);
                    c.setTieneBarco(true);
                    c.setIdBarcoStr(b.id);
                    c.setEsAliado(b.esAliado);

                    c.setEsProa(celda[2] == 1);
                    c.setIndiceEnBarco(celda[3]);
                    c.setTipoBarco(b.tipo);
                    c.setDireccion(dir);

                    if (b.slug != null) c.setSlug(b.slug);

                    if (esSel) c.setSeleccionado(true);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void configurarBotones() {
        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));
        findViewById(R.id.btnMainAttack).setOnClickListener(v -> mostrar(layAtk));
        btnPasarTurno.setOnClickListener(v -> { if(gestor!=null) gestor.pasarTurno(); });

        btnPause.setOnClickListener(v -> {
            new AlertDialog.Builder(this).setTitle("Pausa").setNegativeButton("Rendirse", (d,w) -> gestor.rendirse()).show();
        });

        View.OnClickListener movListener = v -> {
            if (idBarcoSeleccionado == null || gestor == null) return;
            BarcoLogico b = gestor.obtenerBarco(idBarcoSeleccionado);
            if (b == null) return;
            String dir = (v.getId() == R.id.btnForward) ? b.orientation : "S";
            gestor.moverBarco(idBarcoSeleccionado, dir);
            mostrar(layMain);
        };
        findViewById(R.id.btnForward).setOnClickListener(movListener);
        findViewById(R.id.btnBackward).setOnClickListener(movListener);

        btnAtk1.setOnClickListener(v -> { tipoAtaque = 1; mostrar(layNoSel); });
        btnAtk2.setOnClickListener(v -> { if(gestor!=null) gestor.lanzarTorpedo(idBarcoSeleccionado); mostrar(layMain); });
        btnAtk3.setOnClickListener(v -> { tipoAtaque = 2; mostrar(layNoSel); });
    }

    private void initViews() {
        layNoSel = findViewById(R.id.txtNoSelection); layMain = findViewById(R.id.layoutMainActions);
        layMove = findViewById(R.id.layoutMoveActions); layAtk = findViewById(R.id.layoutAttackActions);
        panelInfoBarco = findViewById(R.id.panelInfoBarco); txtInfoTitulo = findViewById(R.id.txtInfoTitulo);
        txtTurnoStatus = findViewById(R.id.txtTurnoStatus); btnPasarTurno = findViewById(R.id.btnPasarTurno);
        btnPause = findViewById(R.id.btnPause); barFuel = findViewById(R.id.barFuel);
        txtFuel = findViewById(R.id.txtFuel); barAmmo = findViewById(R.id.barAmmo); txtAmmo = findViewById(R.id.txtAmmo);
        btnAtk1 = findViewById(R.id.btnAtk1); btnAtk2 = findViewById(R.id.btnAtk2); btnAtk3 = findViewById(R.id.btnAtk3);
    }

    private void mostrar(View v) {
        layNoSel.setVisibility(View.GONE); layMain.setVisibility(View.GONE);
        layAtk.setVisibility(View.GONE); layMove.setVisibility(View.GONE);
        if(v != null) v.setVisibility(View.VISIBLE);
    }
}