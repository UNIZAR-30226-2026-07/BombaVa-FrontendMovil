package com.example.bombavafrontmovil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
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
    // UI
    private View layNoSel, layMain, layAtk, layMove;
    private Button btnAtk2, btnAtk3;
    private android.widget.ProgressBar barFuel, barAmmo;
    private android.widget.TextView txtFuel, txtAmmo;

    // Panel Info
    private View panelInfoBarco;
    private android.widget.TextView txtInfoTitulo, txtInfoGlobal, txtInfoCeldas;

    // Lógica
    private GestorJuego gestor;
    private List<Casilla> matriz = new ArrayList<>();
    private BoardAdapter adapter;
    private Casilla sel = null;

    // Estado
    private boolean modoAtaque = false;
    private int danoEnEspera = 0;
    private int combustible = 10;
    private int municion = 10;

    // Sockets
    private Socket mSocket;
    private android.app.Dialog dialogoEspera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        // 1. INICIALIZAR LÓGICA
        gestor = new GestorJuego();
        for (int i = 0; i < 225; i++) matriz.add(new Casilla(i / 15, i % 15));

        // 2. VINCULAR VISTAS
        initViews();
        actualizarRecursosVisuales();

        // 3. CONFIGURAR TABLERO
        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));
        rv.setItemAnimator(null);

        actualizarMatrizVisual(); // Pinta la flota inicial del gestor

        adapter = new BoardAdapter(matriz, c -> {
            if (modoAtaque) {
                ejecutarAtaque(c);
            } else {
                // 1. Guardamos ID del barco anterior
                int idBarcoAnterior = (sel != null && sel.isTieneBarco()) ? sel.getIdBarco() : -1;
                // Guardamos índice si era agua (para despintar la selección de agua)
                int idxAguaAnterior = (sel != null && !sel.isTieneBarco()) ? sel.getFila() * 15 + sel.getColumna() : -1;

                // 2. Actualizamos lógica
                sel = c;
                actualizarMatrizVisual(); // Actualiza datos, PERO YA NO REPINTA

                // 3. Guardamos ID del nuevo barco
                int idBarcoNuevo = (sel != null && sel.isTieneBarco()) ? sel.getIdBarco() : -1;
                int idxAguaNuevo = (sel != null && !sel.isTieneBarco()) ? sel.getFila() * 15 + sel.getColumna() : -1;

                // --- OPTIMIZACIÓN MÁXIMA ---

                // A) Si había un barco seleccionado antes, lo "apagamos"
                if (idBarcoAnterior != -1) {
                    BarcoLogico bAnt = gestor.obtenerBarco(idBarcoAnterior);
                    if (bAnt != null) {
                        for (int[] pos : bAnt.getCeldas()) {
                            adapter.notifyItemChanged(pos[0] * 15 + pos[1]);
                        }
                    }
                }

                // B) Si hemos seleccionado un barco nuevo, lo "encendemos"
                if (idBarcoNuevo != -1 && idBarcoNuevo != idBarcoAnterior) {
                    BarcoLogico bNuevo = gestor.obtenerBarco(idBarcoNuevo);
                    if (bNuevo != null) {
                        for (int[] pos : bNuevo.getCeldas()) {
                            adapter.notifyItemChanged(pos[0] * 15 + pos[1]);
                        }
                    }
                }

                // C) Gestión de selecciones en el agua (clics fallidos)
                if (idxAguaAnterior != -1) adapter.notifyItemChanged(idxAguaAnterior);
                if (idxAguaNuevo != -1) adapter.notifyItemChanged(idxAguaNuevo);

                mostrar((c.isTieneBarco() && c.isEsAliado()) ? layMain : layNoSel);
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

            if (municion > 0) {
                modoAtaque = true;
                danoEnEspera = barcoReal.danoArmas[index];
                Toast.makeText(this, "🎯 Selecciona objetivo enemigo", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "¡Sin munición!", Toast.LENGTH_SHORT).show();
            }
        };
        findViewById(R.id.btnAtk1).setOnClickListener(accionAtk);
        findViewById(R.id.btnAtk2).setOnClickListener(accionAtk);
        findViewById(R.id.btnAtk3).setOnClickListener(accionAtk);
        findViewById(R.id.btnAtk4).setOnClickListener(accionAtk);
        findViewById(R.id.btnAtk5).setOnClickListener(accionAtk);

        // BOTONES DE MOVIMIENTO (Logic)
        View.OnClickListener accionMov = v -> {
            if (combustible > 0) {
                int accion = 0;
                if (v.getId() == R.id.btnForward) accion = 1;
                else if (v.getId() == R.id.btnBackward) accion = 2;
                else if (v.getId() == R.id.btnRotateL) accion = 3;
                else if (v.getId() == R.id.btnRotateR) accion = 4;

                intentarMovimiento(accion);
                mostrar(layMain);
            } else {
                Toast.makeText(this, "¡Sin combustible!", Toast.LENGTH_SHORT).show();
            }
        };
        findViewById(R.id.btnForward).setOnClickListener(accionMov);
        findViewById(R.id.btnBackward).setOnClickListener(accionMov);
        findViewById(R.id.btnRotateL).setOnClickListener(accionMov);
        findViewById(R.id.btnRotateR).setOnClickListener(accionMov);
    }

    private void configurarBotonInfo() {
        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            if (sel != null && sel.isTieneBarco()) {
                BarcoLogico b = gestor.obtenerBarco(sel.getIdBarco());
                if (b != null) {
                    txtInfoTitulo.setText(b.esAliado ? "NUESTRA NAVE (Tipo " + b.tipo + ")" : "ENEMIGO (Tipo " + b.tipo + ")");
                    txtInfoGlobal.setText("Estructura: " + Math.max(0, b.vidaGeneral) + " HP");

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < b.tipo; i++) {
                        sb.append("Arma ").append(i + 1).append(": ");
                        sb.append(b.vidaCeldas[i] <= 0 ? "ROTA ❌\n" : b.vidaCeldas[i] + "/3 HP 🟢\n");
                    }
                    txtInfoCeldas.setText(sb.toString());
                    panelInfoBarco.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void intentarMovimiento(int accion) {
        BarcoLogico barcoReal = gestor.obtenerBarco(sel.getIdBarco());
        if (barcoReal == null) return;

        // 1. Guardamos DÓNDE ESTABA (para borrarlo de ahí visualmente)
        List<Integer> indicesCambio = new ArrayList<>();
        for (int[] c : barcoReal.getCeldas()) indicesCambio.add(c[0] * 15 + c[1]);

        // 2. Intentamos mover
        boolean exito = gestor.intentarMoverBarco(barcoReal, accion, matriz);

        if (exito) {
            combustible--;
            actualizarRecursosVisuales();
            Toast.makeText(this, "Movimiento completado", Toast.LENGTH_SHORT).show();

            // 3. Guardamos DÓNDE ESTÁ AHORA (para pintarlo en el nuevo sitio)
            for (int[] c : barcoReal.getCeldas()) {
                int idx = c[0] * 15 + c[1];
                // Solo añadimos si no estaba ya en la lista (evitar duplicados en rotaciones)
                if (!indicesCambio.contains(idx)) indicesCambio.add(idx);
            }

            // 4. Actualizamos datos y repintamos SOLO esas celdas
            actualizarMatrizVisual(); // Ahora es "silenciosa"
            if(adapter != null) {
                for(int i : indicesCambio) adapter.notifyItemChanged(i);
            }
        } else {
            Toast.makeText(this, "Bloqueado (Límite o Choque)", Toast.LENGTH_SHORT).show();
        }
    }

    private void ejecutarAtaque(Casilla objetivo) {
        modoAtaque = false;
        municion--;
        actualizarRecursosVisuales();

        List<Integer> afectados = new ArrayList<>();
        afectados.add(objetivo.getFila() * 15 + objetivo.getColumna());

        if (objetivo.isTieneBarco()) {
            BarcoLogico enemigo = gestor.obtenerBarco(objetivo.getIdBarco());
            if (enemigo != null && !enemigo.esAliado) {
                // Añadimos todo el barco enemigo a la lista de repintado
                for (int[] c : enemigo.getCeldas()) afectados.add(c[0] * 15 + c[1]);

                int idxCelda = objetivo.getIndiceEnBarco();
                if (enemigo.vidaCeldas[idxCelda] > 0) enemigo.vidaCeldas[idxCelda]--;
                enemigo.vidaGeneral -= danoEnEspera;

                String msj = "¡IMPACTO! -" + danoEnEspera;
                if (enemigo.vidaCeldas[idxCelda] == 0) msj += "\n¡Arma destruida!";
                Toast.makeText(this, msj, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Fuego amigo no permitido", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "¡Agua!", Toast.LENGTH_SHORT).show();
        }

        actualizarMatrizVisual();
        if(adapter != null) for(int i : afectados) adapter.notifyItemChanged(i);
        mostrar(layMain);
    }

    private void actualizarMatrizVisual() {
        // 1. Limpiamos el tablero (DATOS)
        int idSel = (sel != null && sel.isTieneBarco()) ? sel.getIdBarco() : -1;
        for (Casilla c : matriz) {
            c.setTipoBarco(0);
            c.setIdBarco(-1);
            c.setEsProa(false);
            c.setSeleccionado(false);
            c.setDireccion(0);
        }

        // 2. Colocamos los barcos de la flota (DATOS)
        for (BarcoLogico b : gestor.getFlota()) {
            for (int[] celda : b.getCeldas()) {
                int idx = celda[0] * 15 + celda[1];

                if (idx >= 0 && idx < matriz.size()) {
                    Casilla c = matriz.get(idx);
                    c.setTipoBarco(b.tipo);
                    c.setIdBarco(b.id);
                    c.setEsProa(celda[2] == 1);
                    c.setIndiceEnBarco(celda[3]);
                    c.setDireccion(b.dir); // Rotación
                    c.setEsAliado(b.esAliado);
                    c.setVidaCelda(b.vidaCeldas[celda[3]]);

                    if (b.id == idSel) {
                        c.setSeleccionado(true);
                        sel = c;
                    }
                }
            }
        }

    }

    private void actualizarRecursosVisuales() {
        barFuel.setProgress(combustible); txtFuel.setText(String.valueOf(combustible));
        barAmmo.setProgress(municion); txtAmmo.setText(String.valueOf(municion));
    }

    private void mostrar(View v) {
        if(layNoSel != null) layNoSel.setVisibility(View.GONE);
        if(layMain != null) layMain.setVisibility(View.GONE);
        if(layAtk != null) layAtk.setVisibility(View.GONE);
        if(layMove != null) layMove.setVisibility(View.GONE);
        v.setVisibility(View.VISIBLE);
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