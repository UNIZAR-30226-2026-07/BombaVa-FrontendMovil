package com.example.bombavafrontmovil;

import android.content.Intent;
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
import com.example.bombavafrontmovil.network.SocketManager;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import io.socket.client.Socket;

public class PantallaJuego extends AppCompatActivity {

    private static final String TAG = "DEBUG_BOMBA";
    private String token, matchId, myUserId;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        // 1. RECOGER DATOS
        token = getIntent().getStringExtra("TOKEN");
        myUserId = getIntent().getStringExtra("USER_ID");
        boolean esHost = getIntent().getBooleanExtra("ES_HOST", false);
        String codigoSala = getIntent().getStringExtra("CODIGO_SALA");
        matchId = getIntent().getStringExtra("MATCH_ID");

        initViews();

        // Inicializar tablero 15x15
        for (int i = 0; i < 225; i++) {
            matriz.add(new Casilla(i / 15, i % 15));
        }

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));
        adapter = new BoardAdapter(matriz, c -> {
            if (gestor == null || !gestor.isEsMiTurno()) {
                Toast.makeText(this, "Espera tu turno", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tipoAtaque > 0) {
                if (idBarcoSeleccionado != null) {
                    if (tipoAtaque == 1) gestor.atacarCanon(idBarcoSeleccionado, c.getColumna(), c.getFila());
                    else if (tipoAtaque == 2) gestor.ponerMina(idBarcoSeleccionado, c.getColumna(), c.getFila());
                    tipoAtaque = 0; mostrar(layMain);
                }
            } else {
                if (c.isTieneBarco() && c.isEsAliado()) {
                    idBarcoSeleccionado = c.getIdBarcoStr();
                    mostrar(layMain);
                } else {
                    idBarcoSeleccionado = null;
                    mostrar(layNoSel);
                }
                actualizarMatrizVisual();
            }
        });
        rv.setAdapter(adapter);

        // 2. SOCKET Y LÓGICA DE SALA
        mSocket = SocketManager.getInstance().getSocket();

        if (esHost && (matchId == null || matchId.isEmpty())) {
            mostrarPopUpEspera(codigoSala);
            configurarEscuchaRival();
        } else if (matchId != null) {
            iniciarGestorJuego(matchId);
        }

        configurarBotones();
    }

    private void iniciarGestorJuego(String mId) {
        this.matchId = mId;
        gestor = new GestorJuego(mSocket, matchId, myUserId, new GestorJuego.PartidaListener() {
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
                            .setMessage(ganadorId.equals(myUserId) ? "¡Victoria!" : "Derrota...")
                            .setCancelable(false)
                            .setPositiveButton("Salir", (d, w) -> finish()).show();
                });
            }
        });
    }

    private void configurarEscuchaRival() {
        mSocket.on("match:ready", args -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    if (dialogoEspera != null) dialogoEspera.dismiss();
                    iniciarGestorJuego(data.getString("matchId"));
                } catch (Exception e) { Log.e(TAG, "Error match:ready", e); }
            });
        });
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
            btnPasarTurno.setEnabled(true); btnPasarTurno.setAlpha(1.0f);
        } else {
            txtTurnoStatus.setText("TURNO RIVAL");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            btnPasarTurno.setEnabled(false); btnPasarTurno.setAlpha(0.5f);
            mostrar(layNoSel);
        }
    }

    private void actualizarMatrizVisual() {
        if (gestor == null) return;
        for (Casilla c : matriz) { c.setTieneBarco(false); c.setSeleccionado(false); }
        for (BarcoLogico b : gestor.getFlota()) {
            boolean esSel = (idBarcoSeleccionado != null && idBarcoSeleccionado.equals(b.id));
            // Suponiendo que BarcoLogico calcula sus celdas
            for (int[] celda : b.getCeldas()) {
                int idx = celda[0] * 15 + celda[1];
                if (idx >= 0 && idx < 225) {
                    Casilla c = matriz.get(idx);
                    c.setTieneBarco(true); c.setIdBarcoStr(b.id); c.setEsAliado(b.esAliado);
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

        // MOVIMIENTO
        View.OnClickListener movListener = v -> {
            if (idBarcoSeleccionado == null || gestor == null) return;
            BarcoLogico b = gestor.obtenerBarco(idBarcoSeleccionado);
            if (b == null) return;
            String dir = (v.getId() == R.id.btnForward) ? b.orientation : "S";
            gestor.moverBarco(idBarcoSeleccionado, dir);
            mostrar(layMain);
        };
        findViewById(R.id.btnForward).setOnClickListener(movListener); findViewById(R.id.btnBackward).setOnClickListener(movListener);

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