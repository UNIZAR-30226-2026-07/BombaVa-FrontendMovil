package com.example.bombavafrontmovil;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PantallaJuego extends AppCompatActivity {

    // --- RECUERDA ACTUALIZAR ESTOS VALORES CON LOS QUE SALGAN EN TU TERMINAL ---
    private static final String TOKEN_TEMPORAL = "TU_TOKEN_AQUI";
    private static final String MATCH_ID_TEMPORAL = "TU_MATCH_ID_AQUI";
    private static final String MY_USER_ID = "TU_USER_ID_AQUI";

    private GestorJuego gestor;
    private List<Casilla> matriz = new ArrayList<>();
    private BoardAdapter adapter;

    // MEMORIA DE SELECCIÓN
    private String idBarcoSeleccionado = null;

    private View layNoSel, layMain, layMove, layAtk;
    private TextView txtInfoTitulo, txtInfoGlobal, txtInfoCeldas, txtTurnoStatus;
    private View panelInfoBarco;
    private Button btnPasarTurno;

    private boolean modoAtaque = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        initViews();

        // Inicializar tablero 15x15
        for (int i = 0; i < 225; i++) matriz.add(new Casilla(i / 15, i % 15));

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));
        rv.setItemAnimator(null);

        adapter = new BoardAdapter(matriz, c -> {
            // BLOQUEO POR TURNO
            if (!gestor.isEsMiTurno()) {
                Toast.makeText(this, "Es el turno del oponente", Toast.LENGTH_SHORT).show();
                return;
            }

            if (modoAtaque) {
                if (idBarcoSeleccionado != null) {
                    // ATAQUE OFICIAL: ship:attack:cannon
                    gestor.atacarCanon(idBarcoSeleccionado, c.getColumna(), c.getFila());
                    Toast.makeText(this, "¡Fuego enviado!", Toast.LENGTH_SHORT).show();
                    modoAtaque = false;
                    mostrar(layMain);
                }
            } else {
                // SELECCIÓN DE BARCO PROPIO
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

        // CONEXIÓN AL GESTOR ADAPTADO AL BACKEND OFICIAL
        gestor = new GestorJuego(TOKEN_TEMPORAL, MATCH_ID_TEMPORAL, MY_USER_ID, () -> {
            runOnUiThread(() -> {
                actualizarInterfazTurno();
                actualizarMatrizVisual();
            });
        });

        configurarBotones();
    }

    private void actualizarInterfazTurno() {
        if (gestor.isEsMiTurno()) {
            txtTurnoStatus.setText("TU TURNO");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            btnPasarTurno.setEnabled(true);
            btnPasarTurno.setAlpha(1.0f);
        } else {
            txtTurnoStatus.setText("TURNO ENEMIGO");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            btnPasarTurno.setEnabled(false);
            btnPasarTurno.setAlpha(0.5f);
            mostrar(layNoSel); // Ocultamos menús de acción si no es nuestro turno
        }
    }

    private void actualizarMatrizVisual() {
        // FOTO ANTES PARA DIFFING (Fluidez)
        String[] idsAnteriores = new String[225];
        boolean[] seleccionAnterior = new boolean[225];
        int[] direccionAnterior = new int[225];

        for (int i = 0; i < 225; i++) {
            Casilla c = matriz.get(i);
            idsAnteriores[i] = c.getIdBarcoStr();
            seleccionAnterior[i] = c.isSeleccionado();
            direccionAnterior[i] = c.getDireccion();
        }

        // RESET LÓGICO
        for (Casilla c : matriz) {
            c.setTieneBarco(false);
            c.setIdBarcoStr(null);
            c.setSeleccionado(false);
        }

        // CARGAR DATOS NUEVOS DEL SERVIDOR
        for (BarcoLogico b : gestor.getFlota()) {
            boolean esElSeleccionado = (idBarcoSeleccionado != null && idBarcoSeleccionado.equals(b.id));

            for (int[] celda : b.getCeldas()) {
                int idx = celda[0] * 15 + celda[1];
                if (idx >= 0 && idx < 225) {
                    Casilla c = matriz.get(idx);
                    c.setTieneBarco(true);
                    c.setIdBarcoStr(b.id);
                    c.setTipoBarco(b.tipo);
                    c.setEsProa(celda[2] == 1);
                    c.setIndiceEnBarco(celda[3]);
                    c.setEsAliado(b.esAliado);

                    int dirNum = 0;
                    if ("E".equals(b.orientation)) dirNum = 1;
                    else if ("S".equals(b.orientation)) dirNum = 2;
                    else if ("W".equals(b.orientation)) dirNum = 3;
                    c.setDireccion(dirNum);

                    if (esElSeleccionado) c.setSeleccionado(true);
                }
            }
        }

        // COMPARAR Y REFRESCAR SOLO LO QUE CAMBIÓ
        for (int i = 0; i < 225; i++) {
            Casilla c = matriz.get(i);
            boolean idCambio = (c.getIdBarcoStr() == null && idsAnteriores[i] != null) ||
                    (c.getIdBarcoStr() != null && !c.getIdBarcoStr().equals(idsAnteriores[i]));
            boolean selCambio = (c.isSeleccionado() != seleccionAnterior[i]);
            boolean dirCambio = (c.getDireccion() != direccionAnterior[i]);

            if (idCambio || selCambio || dirCambio) {
                adapter.notifyItemChanged(i);
            }
        }
    }

    private void configurarBotones() {
        // MENÚS
        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));
        findViewById(R.id.btnMainAttack).setOnClickListener(v -> mostrar(layAtk));

        // PASAR TURNO (match:turn_end)
        btnPasarTurno.setOnClickListener(v -> {
            gestor.pasarTurno();
            Toast.makeText(this, "Fin del turno", Toast.LENGTH_SHORT).show();
        });

        // MOVIMIENTO (ship:move)
        View.OnClickListener movListener = v -> {
            if (idBarcoSeleccionado == null) return;
            String dir = "N";
            if (v.getId() == R.id.btnBackward) dir = "S";
            else if (v.getId() == R.id.btnRotateL) dir = "W";
            else if (v.getId() == R.id.btnRotateR) dir = "E";

            gestor.moverBarco(idBarcoSeleccionado, dir);
            mostrar(layMain);
        };
        findViewById(R.id.btnForward).setOnClickListener(movListener);
        findViewById(R.id.btnBackward).setOnClickListener(movListener);
        findViewById(R.id.btnRotateL).setOnClickListener(movListener);
        findViewById(R.id.btnRotateR).setOnClickListener(movListener);

        // ATAQUE
        findViewById(R.id.btnAtk1).setOnClickListener(v -> {
            modoAtaque = true;
            Toast.makeText(this, "🎯 Selecciona objetivo en el mapa", Toast.LENGTH_SHORT).show();
            mostrar(layNoSel);
        });

        // INFO
        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            if (idBarcoSeleccionado != null) {
                BarcoLogico b = gestor.obtenerBarco(idBarcoSeleccionado);
                if (b != null) {
                    txtInfoTitulo.setText("ID: " + b.id.substring(0, 8));
                    txtInfoGlobal.setText("Bando: " + (b.esAliado ? "Aliado" : "Enemigo"));
                    txtInfoCeldas.setText("Pos: " + b.x + "," + b.y + "\nDir: " + b.orientation);
                    panelInfoBarco.setVisibility(View.VISIBLE);
                }
            }
        });
        findViewById(R.id.btnCloseInfo).setOnClickListener(v -> panelInfoBarco.setVisibility(View.GONE));
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

        // ELEMENTOS DE TURNO
        txtTurnoStatus = findViewById(R.id.txtTurnoStatus); // Añade este TextView a tu XML
        btnPasarTurno = findViewById(R.id.btnPasarTurno);   // Añade este Button a tu XML
    }

    private void mostrar(View v) {
        if(layNoSel != null) layNoSel.setVisibility(View.GONE);
        if(layMain != null) layMain.setVisibility(View.GONE);
        if(layAtk != null) layAtk.setVisibility(View.GONE);
        if(layMove != null) layMove.setVisibility(View.GONE);
        if(v != null) v.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gestor != null) gestor.desconectar();
    }
}