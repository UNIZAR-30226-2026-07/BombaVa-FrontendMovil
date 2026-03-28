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

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class PantallaJuego extends AppCompatActivity {

    private String token;
    private String matchId;
    private String myUserId;

    private GestorJuego gestor;
    private List<Casilla> matriz = new ArrayList<>();
    private BoardAdapter adapter;
    private String idBarcoSeleccionado = null;

    // Vistas principales
    private View layNoSel, layMain, layMove, layAtk;
    private TextView txtInfoTitulo, txtInfoGlobal, txtInfoCeldas, txtTurnoStatus;
    private View panelInfoBarco;
    private Button btnPasarTurno;
    private ImageButton btnPause;

    // Armas
    private Button btnAtk1, btnAtk2, btnAtk3;
    private View divAtk2, divAtk3;

    // Recursos
    private ProgressBar barFuel, barAmmo;
    private TextView txtFuel, txtAmmo;

    // Control de tipo de ataque: 0=Ninguno, 1=Cañón, 2=Mina (Torpedo es inmediato)
    private int tipoAtaque = 0;

    // Sockets
    private Socket mSocket;
    private android.app.Dialog dialogoEspera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        // RECOGER DATOS (Automático o manual para debug)
        token = getIntent().getStringExtra("TOKEN");
        matchId = getIntent().getStringExtra("MATCH_ID");
        myUserId = getIntent().getStringExtra("USER_ID");

        // --- MODO DEBUG SI ESTÁN VACÍOS ---
        if(token == null) token = "PON_TU_TOKEN_AQUI";
        if(matchId == null) matchId = "PON_TU_MATCH_ID_AQUI";
        if(myUserId == null) myUserId = "PON_TU_USER_ID_AQUI";

        initViews();
        for (int i = 0; i < 225; i++) matriz.add(new Casilla(i / 15, i % 15));

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));
        rv.setItemAnimator(null);

        adapter = new BoardAdapter(matriz, c -> {
            if (!gestor.isEsMiTurno()) {
                Toast.makeText(this, "Espera tu turno", Toast.LENGTH_SHORT).show();
                return;
            }

            if (tipoAtaque > 0) {
                if (idBarcoSeleccionado != null) {
                    if (tipoAtaque == 1) {
                        gestor.atacarCanon(idBarcoSeleccionado, c.getColumna(), c.getFila());
                        Toast.makeText(this, "¡Cañonazo!", Toast.LENGTH_SHORT).show();
                    } else if (tipoAtaque == 2) {
                        gestor.ponerMina(idBarcoSeleccionado, c.getColumna(), c.getFila());
                        Toast.makeText(this, "¡Mina desplegada!", Toast.LENGTH_SHORT).show();
                    }
                    tipoAtaque = 0;
                    mostrar(layMain);
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

        // 4. CONFIGURAR BOTONES
        configurarBotonesAccion();
        configurarBotonInfo();

        // Recuperamos la instancia del socket
        mSocket = SocketManager.getInstance().getSocket();

        // Leemos los datos que nos envía la pantalla anterior
        Intent intent = getIntent();
        boolean esHost = intent.getBooleanExtra("ES_HOST", false);
        String codigoSala = intent.getStringExtra("CODIGO_SALA");
        String matchId = intent.getStringExtra("MATCH_ID"); // Vendrá lleno si somos el invitado

        if (esHost && codigoSala != null) {
            // SOMOS EL CREADOR: Mostramos el Pop-up y esperamos
            mostrarPopUpEspera(codigoSala);
            configurarEscuchaRival();
        } else if (matchId != null) {
            // SOMOS EL INVITADO: Ya tenemos el ID del match, nos unimos directamente al juego
            unirseAlJuegoReal(matchId);
        }
    }

    private void initViews() {
        layNoSel = findViewById(R.id.txtNoSelection);
        layMain = findViewById(R.id.layoutMainActions);
        layAtk = findViewById(R.id.layoutAttackActions);
        layMove = findViewById(R.id.layoutMoveActions);

        btnAtk2 = findViewById(R.id.btnAtk2);
        btnAtk3 = findViewById(R.id.btnAtk3);

        barFuel = findViewById(R.id.barFuel);
        barAmmo = findViewById(R.id.barAmmo);
        txtFuel = findViewById(R.id.txtFuel);
        txtAmmo = findViewById(R.id.txtAmmo);

        barFuel.setMax(10); barAmmo.setMax(10);

        // Referencias panel info
        panelInfoBarco = findViewById(R.id.panelInfoBarco);
        txtInfoTitulo = findViewById(R.id.txtInfoTitulo);
        txtInfoGlobal = findViewById(R.id.txtInfoGlobal);
        txtInfoCeldas = findViewById(R.id.txtInfoCeldas);
        findViewById(R.id.btnCloseInfo).setOnClickListener(v -> panelInfoBarco.setVisibility(View.GONE));
    }

    private void configurarBotonesAccion() {
        // ATAQUE
        findViewById(R.id.btnMainAttack).setOnClickListener(v -> {
            int size = sel.getTipoBarco();
            // Control de visibilidad de botones de ataque
            btnAtk2.setVisibility(size >= 2 ? View.VISIBLE : View.GONE);
            findViewById(R.id.divAtk2).setVisibility(size >= 2 ? View.VISIBLE : View.GONE);
            btnAtk3.setVisibility(size >= 3 ? View.VISIBLE : View.GONE);
            findViewById(R.id.divAtk3).setVisibility(size >= 3 ? View.VISIBLE : View.GONE);
            findViewById(R.id.btnAtk4).setVisibility(size >= 4 ? View.VISIBLE : View.GONE);
            findViewById(R.id.divAtk4).setVisibility(size >= 4 ? View.VISIBLE : View.GONE);
            findViewById(R.id.btnAtk5).setVisibility(size == 5 ? View.VISIBLE : View.GONE);
            findViewById(R.id.divAtk5).setVisibility(size == 5 ? View.VISIBLE : View.GONE);

            mostrar(layAtk);
        });

        // MOVIMIENTO
        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));

        // BOTONES DE ATAQUE (Logic)
        View.OnClickListener accionAtk = v -> {
            int index = 0;
            if (v.getId() == R.id.btnAtk2) index = 1;
            if (v.getId() == R.id.btnAtk3) index = 2;
            if (v.getId() == R.id.btnAtk4) index = 3;
            if (v.getId() == R.id.btnAtk5) index = 4;

            BarcoLogico barcoReal = gestor.obtenerBarco(sel.getIdBarco());
            if (barcoReal == null) return;

            if (barcoReal.vidaCeldas[index] <= 0) {
                Toast.makeText(this, "⚠️ Arma destruida.", Toast.LENGTH_SHORT).show();
                return;
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
                runOnUiThread(() -> mostrarDialogoFin(ganadorId, razon));
            }
        });

        configurarBotones();
    }

    private void mostrarDialogoFin(String ganadorId, String razon) {
        boolean heGanado = ganadorId.equals(myUserId);
        String titulo = heGanado ? "¡VICTORIA! 🏆" : "¡DERROTA! 💥";
        String mensaje = heGanado ? "Has destruido a la flota enemiga." : "Tu flota yace en el fondo del mar.";

        if ("surrender".equals(razon)) {
            mensaje = heGanado ? "El enemigo se ha rendido cobardemente." : "Te has rendido.";
        }

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setCancelable(false)
                .setPositiveButton("Volver al Puerto", (dialog, which) -> finish())
                .show();
    }

    private void actualizarInterfazTurno() {
        if (gestor.isEsMiTurno()) {
            txtTurnoStatus.setText("TU TURNO");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            btnPasarTurno.setEnabled(true); btnPasarTurno.setAlpha(1.0f);
        } else {
            txtTurnoStatus.setText("TURNO ENEMIGO");
            txtTurnoStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            btnPasarTurno.setEnabled(false); btnPasarTurno.setAlpha(0.5f);
            mostrar(layNoSel);
        }
    }

    private void actualizarMatrizVisual() {
        String[] idsAnt = new String[225]; boolean[] selAnt = new boolean[225]; int[] dirAnt = new int[225];
        for (int i = 0; i < 225; i++) {
            Casilla c = matriz.get(i);
            idsAnt[i] = c.getIdBarcoStr(); selAnt[i] = c.isSeleccionado(); dirAnt[i] = c.getDireccion();
            c.setTieneBarco(false); c.setIdBarcoStr(null); c.setSeleccionado(false);
        }

        for (BarcoLogico b : gestor.getFlota()) {
            boolean esSel = (idBarcoSeleccionado != null && idBarcoSeleccionado.equals(b.id));
            for (int[] celda : b.getCeldas()) {
                int idx = celda[0] * 15 + celda[1];
                if (idx >= 0 && idx < 225) {
                    Casilla c = matriz.get(idx);
                    c.setTieneBarco(true); c.setIdBarcoStr(b.id); c.setTipoBarco(b.tipo);
                    c.setEsProa(celda[2] == 1); c.setIndiceEnBarco(celda[3]); c.setEsAliado(b.esAliado);

                    int dirNum = 0;
                    if ("E".equals(b.orientation)) dirNum = 1; else if ("S".equals(b.orientation)) dirNum = 2; else if ("W".equals(b.orientation)) dirNum = 3;
                    c.setDireccion(dirNum);
                    if (esSel) c.setSeleccionado(true);
                }
            }
        }

        for (int i = 0; i < 225; i++) {
            Casilla c = matriz.get(i);
            boolean idCambio = (c.getIdBarcoStr() == null && idsAnt[i] != null) || (c.getIdBarcoStr() != null && !c.getIdBarcoStr().equals(idsAnt[i]));
            if (idCambio || c.isSeleccionado() != selAnt[i] || c.getDireccion() != dirAnt[i]) {
                adapter.notifyItemChanged(i);
            }
        }
    }

    private void configurarBotones() {
        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));
        findViewById(R.id.btnMainAttack).setOnClickListener(v -> mostrar(layAtk));

        btnPasarTurno.setOnClickListener(v -> gestor.pasarTurno());

        // PAUSA / RENDIRSE
        btnPause.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Pausa Táctica")
                    .setMessage("¿Qué deseas hacer Comandante?")
                    .setPositiveButton("Reanudar", null)
                    .setNegativeButton("Huir (Rendirse)", (dialog, which) -> gestor.rendirse())
                    .show();
        });

        // MOVIMIENTO
        View.OnClickListener movListener = v -> {
            if (idBarcoSeleccionado == null) return;
            String dir = "N";
            if (v.getId() == R.id.btnBackward) dir = "S"; else if (v.getId() == R.id.btnRotateL) dir = "W"; else if (v.getId() == R.id.btnRotateR) dir = "E";
            gestor.moverBarco(idBarcoSeleccionado, dir);
            mostrar(layMain);
        };
        findViewById(R.id.btnForward).setOnClickListener(movListener); findViewById(R.id.btnBackward).setOnClickListener(movListener);
        findViewById(R.id.btnRotateL).setOnClickListener(movListener); findViewById(R.id.btnRotateR).setOnClickListener(movListener);

        // ARMAS
        btnAtk1.setOnClickListener(v -> { tipoAtaque = 1; Toast.makeText(this, "🎯 Toca el objetivo del Cañón", Toast.LENGTH_SHORT).show(); mostrar(layNoSel); });

        btnAtk2.setOnClickListener(v -> {
            // El torpedo no necesita objetivo, va recto
            gestor.lanzarTorpedo(idBarcoSeleccionado);
            Toast.makeText(this, "🚀 Torpedo en el agua!", Toast.LENGTH_SHORT).show();
            mostrar(layMain);
        });

        btnAtk3.setOnClickListener(v -> { tipoAtaque = 2; Toast.makeText(this, "💣 Toca una casilla adyacente para la Mina", Toast.LENGTH_SHORT).show(); mostrar(layNoSel); });

        // INFO
        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            if (idBarcoSeleccionado != null) {
                BarcoLogico b = gestor.obtenerBarco(idBarcoSeleccionado);
                if (b != null) {
                    txtInfoTitulo.setText("ID: " + b.id.substring(0, 8));
                    txtInfoCeldas.setText("Pos: " + b.x + "," + b.y + "\nDir: " + b.orientation);
                    panelInfoBarco.setVisibility(View.VISIBLE);
                }
            }
        });
        findViewById(R.id.btnCloseInfo).setOnClickListener(v -> panelInfoBarco.setVisibility(View.GONE));
    }

    private void initViews() {
        layNoSel = findViewById(R.id.txtNoSelection); layMain = findViewById(R.id.layoutMainActions);
        layMove = findViewById(R.id.layoutMoveActions); layAtk = findViewById(R.id.layoutAttackActions);
        panelInfoBarco = findViewById(R.id.panelInfoBarco); txtInfoTitulo = findViewById(R.id.txtInfoTitulo);
        txtInfoGlobal = findViewById(R.id.txtInfoGlobal); txtInfoCeldas = findViewById(R.id.txtInfoCeldas);

        txtTurnoStatus = findViewById(R.id.txtTurnoStatus); btnPasarTurno = findViewById(R.id.btnPasarTurno);
        btnPause = findViewById(R.id.btnPause);

        // Recursos (Ajustamos max para mayor precisión, el backend manda números grandes)
        barFuel = findViewById(R.id.barFuel); txtFuel = findViewById(R.id.txtFuel);
        barAmmo = findViewById(R.id.barAmmo); txtAmmo = findViewById(R.id.txtAmmo);
        barFuel.setMax(100); barAmmo.setMax(20);

        // Armas (Hacemos visibles los botones 2 y 3 del XML y cambiamos textos)
        btnAtk1 = findViewById(R.id.btnAtk1); btnAtk1.setText("Cañón");
        btnAtk2 = findViewById(R.id.btnAtk2); btnAtk2.setVisibility(View.VISIBLE); btnAtk2.setText("Torpedo");
        btnAtk3 = findViewById(R.id.btnAtk3); btnAtk3.setVisibility(View.VISIBLE); btnAtk3.setText("Mina");

        divAtk2 = findViewById(R.id.divAtk2); divAtk2.setVisibility(View.VISIBLE);
        divAtk3 = findViewById(R.id.divAtk3); divAtk3.setVisibility(View.VISIBLE);
    }

    private void mostrar(View v) {
        layNoSel.setVisibility(View.GONE); layMain.setVisibility(View.GONE);
        layAtk.setVisibility(View.GONE); layMove.setVisibility(View.GONE);
        if(v != null) v.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gestor != null) gestor.desconectar();
    }

    private void mostrarPopUpEspera(String codigo) {
        // Crear el diálogo personalizado
        dialogoEspera = new android.app.Dialog(this);
        dialogoEspera.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialogoEspera.setContentView(R.layout.dialog_codigo_partida);

        // Esto impide que el usuario cierre el pop-up tocando fuera
        dialogoEspera.setCancelable(false);

        // Hacer fondo transparente
        if (dialogoEspera.getWindow() != null) {
            dialogoEspera.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Vincular las vistas del XML del popup
        android.widget.TextView tvCodigo = dialogoEspera.findViewById(R.id.tv_codigo_generado);
        android.widget.Button btnCopiar = dialogoEspera.findViewById(R.id.btn_copiar_codigo);
        android.widget.Button btnCerrar = dialogoEspera.findViewById(R.id.btn_cerrar_popup);

        // Mostrar el código real generado por el servidor
        tvCodigo.setText(codigo);

        // Configurar el botón de COPIAR
        btnCopiar.setOnClickListener(v -> {
            // Lógica real para copiar al portapapeles del móvil
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Código de Partida", codigo);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "¡Código copiado al portapapeles!", Toast.LENGTH_SHORT).show();
        });

        // Configurar el botón de CERRAR
        btnCerrar.setOnClickListener(v -> {
            dialogoEspera.dismiss();
            Toast.makeText(this, "Esperando rival en segundo plano...", Toast.LENGTH_SHORT).show();
            // Nota: Si quieres que al cerrar el popup se cancele la partida y vuelva atrás,
            // puedes poner aquí un 'finish();' en su lugar.
            finish();
        });

        // Mostrar el popup
        dialogoEspera.show();
    }

    private void configurarEscuchaRival() {
        mSocket.on("match:ready", args -> {
            runOnUiThread(() -> {
                try {
                    // El servidor avisa que el rival ha entrado
                    JSONObject data = (JSONObject) args[0];
                    String matchId = data.getString("matchId");

                    // Cerramos el Pop-up!
                    if (dialogoEspera != null && dialogoEspera.isShowing()) {
                        dialogoEspera.dismiss();
                    }

                    Toast.makeText(this, "¡El rival se ha unido! A los cañones.", Toast.LENGTH_SHORT).show();

                    // Nos conectamos al tablero de juego del servidor
                    unirseAlJuegoReal(matchId);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void unirseAlJuegoReal(String matchId) {
        try {
            // Según la documentación de tu API, mandamos game:join
            JSONObject payload = new JSONObject();
            payload.put("matchId", matchId);
            mSocket.emit("game:join", payload);

            // Opcional: escuchar si la conexión al tablero fue exitosa
            mSocket.on("game:joined", args -> {
                runOnUiThread(() -> Toast.makeText(this, "Conectado al tablero.", Toast.LENGTH_SHORT).show());
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.off("match:ready");
            mSocket.off("game:joined");
        }
    }
}