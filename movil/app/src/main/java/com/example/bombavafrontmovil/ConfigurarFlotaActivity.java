package com.example.bombavafrontmovil;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurarFlotaActivity extends AppCompatActivity {

    public static class CeldaVisual {
        int colorAgua;
        int idImagenBarco = 0;
        float rotacion = 0f;
        boolean seleccionadaParaArma = false;

        public CeldaVisual(int colorAgua) {
            this.colorAgua = colorAgua;
        }
    }

    private ConfigurarFlotaAdapter adaptador;
    private List<CeldaVisual> celdasTablero;
    private int[] casillasOcupadas = new int[225];

    private Map<Integer, String> armasEquipadasPorBarco = new HashMap<>();

    private boolean enHorizontal = true;
    private int tamanoSeleccionado = 0;
    private int idBarcoActual = 1;
    private int faseActual = 1;

    private int idBarcoSeleccionadoParaArma = 0;
    private String armaTemporal = "";

    private boolean barco5Colocado = false, barco3Colocado = false, barco1Colocado = false;

    private LinearLayout layoutControlesColocacion, layoutControlesArmas;

    private Button btnConfirmar, btnRotar, btnShip5, btnShip3, btnShip1;
    private Button btnAmetralladora, btnMisil, btnTorpedo, btnCancelarArma, btnGuardarArma;

    private int colorAguaEnemiga, colorAguaNeutra, colorAguaAliada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_flota);

        colorAguaEnemiga = Color.parseColor("#90CAF9");
        colorAguaNeutra = Color.parseColor("#4FC3F7");
        colorAguaAliada = ContextCompat.getColor(this, R.color.sea_blue);

        vincularVistas();
        configurarTablero();
        configurarListeners();
    }

    private void vincularVistas() {
        layoutControlesColocacion = findViewById(R.id.layout_controles);
        layoutControlesArmas = findViewById(R.id.layout_controles_armas);
        btnConfirmar = findViewById(R.id.btn_confirmar);

        btnShip5 = findViewById(R.id.btn_ship_5x1);
        btnShip3 = findViewById(R.id.btn_ship_3x1);
        btnShip1 = findViewById(R.id.btn_ship_1x1);
        btnRotar = findViewById(R.id.btn_rotate);
        btnRotar.setText("Rotar (H)");

        btnAmetralladora = findViewById(R.id.btn_ametralladora);
        btnMisil = findViewById(R.id.btn_misil);
        btnTorpedo = findViewById(R.id.btn_torpedo);
        btnCancelarArma = findViewById(R.id.btn_cancelar_arma);
        btnGuardarArma = findViewById(R.id.btn_guardar_arma);
    }

    private void configurarTablero() {
        RecyclerView rvTablero = findViewById(R.id.rvBoard);
        celdasTablero = new ArrayList<>();

        for (int i = 0; i < 225; i++) {
            int fila = i / 15;
            int colorBase = (fila < 5) ? colorAguaEnemiga : ((fila < 10) ? colorAguaNeutra : colorAguaAliada);
            celdasTablero.add(new CeldaVisual(colorBase));
        }

        rvTablero.setHasFixedSize(true);
        rvTablero.setItemAnimator(null);

        rvTablero.setLayoutManager(new GridLayoutManager(this, 15));
        adaptador = new ConfigurarFlotaAdapter(celdasTablero, this::manejarToqueCelda);
        rvTablero.setAdapter(adaptador);
    }

    private void configurarListeners() {
        btnShip5.setOnClickListener(v -> { tamanoSeleccionado = 5; actualizarBotonesSeleccionColocacion(5); });
        btnShip3.setOnClickListener(v -> { tamanoSeleccionado = 3; actualizarBotonesSeleccionColocacion(3); });
        btnShip1.setOnClickListener(v -> { tamanoSeleccionado = 1; actualizarBotonesSeleccionColocacion(1); });

        btnRotar.setOnClickListener(v -> {
            enHorizontal = !enHorizontal;
            btnRotar.setText(enHorizontal ? "Rotar (H)" : "Rotar (V)");
        });

        btnAmetralladora.setOnClickListener(v -> seleccionarArmaTemporal("Ametralladora"));
        btnMisil.setOnClickListener(v -> seleccionarArmaTemporal("Misil"));
        btnTorpedo.setOnClickListener(v -> seleccionarArmaTemporal("Torpedo"));

        btnCancelarArma.setOnClickListener(v -> cancelarSeleccionArma());
        btnGuardarArma.setOnClickListener(v -> guardarArmaBarco());

        btnConfirmar.setOnClickListener(v -> avanzarFase());
    }

    private void seleccionarArmaTemporal(String tipo) {
        armaTemporal = tipo;
        actualizarBotonesArmaTemporal();
    }

    private void actualizarBotonesArmaTemporal() {
        btnAmetralladora.setAlpha(1.0f); btnMisil.setAlpha(1.0f); btnTorpedo.setAlpha(1.0f);
        btnAmetralladora.getBackground().mutate().clearColorFilter();
        btnMisil.getBackground().mutate().clearColorFilter();
        btnTorpedo.getBackground().mutate().clearColorFilter();

        if (armaTemporal.equals("Ametralladora")) {
            btnAmetralladora.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            btnMisil.setAlpha(0.6f); btnTorpedo.setAlpha(0.6f);
        } else if (armaTemporal.equals("Misil")) {
            btnMisil.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            btnAmetralladora.setAlpha(0.6f); btnTorpedo.setAlpha(0.6f);
        } else if (armaTemporal.equals("Torpedo")) {
            btnTorpedo.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            btnAmetralladora.setAlpha(0.6f); btnMisil.setAlpha(0.6f);
        }
    }

    private void guardarArmaBarco() {
        if (idBarcoSeleccionadoParaArma != 0) {
            if (armaTemporal.isEmpty()) {
                Toast.makeText(this, "Selecciona un arma o pulsa Cancelar para no hacer cambios", Toast.LENGTH_SHORT).show();
                return;
            }
            armasEquipadasPorBarco.put(idBarcoSeleccionadoParaArma, armaTemporal);
            Toast.makeText(this, armaTemporal + " equipada al barco!", Toast.LENGTH_SHORT).show();
            cancelarSeleccionArma();
        }
    }

    private void actualizarBotonesSeleccionColocacion(int tamano) {
        btnShip5.setAlpha(1.0f); btnShip3.setAlpha(1.0f); btnShip1.setAlpha(1.0f);
        btnShip5.getBackground().mutate().clearColorFilter();
        btnShip3.getBackground().mutate().clearColorFilter();
        btnShip1.getBackground().mutate().clearColorFilter();

        if (tamano == 5) {
            btnShip5.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            btnShip3.setAlpha(0.6f); btnShip1.setAlpha(0.6f);
        } else if (tamano == 3) {
            btnShip3.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            btnShip5.setAlpha(0.6f); btnShip1.setAlpha(0.6f);
        } else if (tamano == 1) {
            btnShip1.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            btnShip5.setAlpha(0.6f); btnShip3.setAlpha(0.6f);
        }
    }

    private void manejarToqueCelda(int pos) {
        if (faseActual == 2) {
            int idBarco = casillasOcupadas[pos];
            if (idBarco != 0) {
                actualizarMarcadorBarco(idBarco);
                idBarcoSeleccionadoParaArma = idBarco;

                // Limpiamos la selección visual para obligarle a elegir de las armas restantes
                armaTemporal = "";

                actualizarVisibilidadBotonesArmas();
                actualizarBotonesArmaTemporal();

                layoutControlesArmas.setVisibility(View.VISIBLE);
            } else {
                cancelarSeleccionArma();
            }
            return;
        }
        colocarBarco(pos);
    }

    // --- MAGIA AQUÍ: Las armas ya equipadas en cualquier barco DESAPARECEN ---
    private void actualizarVisibilidadBotonesArmas() {
        boolean ametralladoraUsada = armasEquipadasPorBarco.containsValue("Ametralladora");
        boolean misilUsado = armasEquipadasPorBarco.containsValue("Misil");
        boolean torpedoUsado = armasEquipadasPorBarco.containsValue("Torpedo");

        btnAmetralladora.setVisibility(ametralladoraUsada ? View.GONE : View.VISIBLE);
        btnMisil.setVisibility(misilUsado ? View.GONE : View.VISIBLE);
        btnTorpedo.setVisibility(torpedoUsado ? View.GONE : View.VISIBLE);
    }

    private void cancelarSeleccionArma() {
        limpiarMarcadorBarco();
        armaTemporal = "";
        layoutControlesArmas.setVisibility(View.GONE);
    }

    private void colocarBarco(int pos) {
        if (casillasOcupadas[pos] != 0) {
            if (tamanoSeleccionado == 0) {
                borrarBarco(casillasOcupadas[pos]);
            } else {
                Toast.makeText(this, "Error: No puedes colocar un barco encima de otro", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (tamanoSeleccionado == 0) return;

        int fila = pos / 15;
        int col = pos % 15;

        if (enHorizontal && (col + tamanoSeleccionado > 15)) {
            col = 15 - tamanoSeleccionado;
            pos = (fila * 15) + col;
        }
        if (!enHorizontal && (fila + tamanoSeleccionado > 15)) {
            fila = 15 - tamanoSeleccionado;
            pos = (fila * 15) + col;
        }

        if (fila < 10) {
            Toast.makeText(this, "ZONA RESTRINGIDA: Debes colocar tu flota en tu zona", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < tamanoSeleccionado; i++) {
            int check = enHorizontal ? (pos + i) : (pos + (i * 15));
            if (casillasOcupadas[check] != 0) {
                Toast.makeText(this, "Error: El barco choca con otro ya colocado", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        float rotacion = enHorizontal ? 90f : 0f;

        int imgProa = R.drawable.barco_proa;
        int imgMedio = R.drawable.barco_medio;
        int imgPopa = R.drawable.barco_popa;
        int imgUnico = R.drawable.barco_medio;

        for (int i = 0; i < tamanoSeleccionado; i++) {
            int p = enHorizontal ? (pos + i) : (pos + (i * 15));
            casillasOcupadas[p] = idBarcoActual;
            CeldaVisual celda = celdasTablero.get(p);

            celda.rotacion = rotacion;

            if (tamanoSeleccionado == 1) {
                celda.idImagenBarco = imgUnico;
            } else {
                if (enHorizontal) {
                    if (i == 0) celda.idImagenBarco = imgPopa;
                    else if (i == tamanoSeleccionado - 1) celda.idImagenBarco = imgProa;
                    else celda.idImagenBarco = imgMedio;
                } else {
                    if (i == 0) celda.idImagenBarco = imgProa;
                    else if (i == tamanoSeleccionado - 1) celda.idImagenBarco = imgPopa;
                    else celda.idImagenBarco = imgMedio;
                }
            }
        }

        if (tamanoSeleccionado == 5) { barco5Colocado = true; btnShip5.setVisibility(View.GONE); }
        if (tamanoSeleccionado == 3) { barco3Colocado = true; btnShip3.setVisibility(View.GONE); }
        if (tamanoSeleccionado == 1) { barco1Colocado = true; btnShip1.setVisibility(View.GONE); }

        idBarcoActual++;
        tamanoSeleccionado = 0;
        actualizarBotonesSeleccionColocacion(0);
        adaptador.notifyDataSetChanged();
    }

    private void borrarBarco(int id) {
        for (int i = 0; i < 225; i++) {
            if (casillasOcupadas[i] == id) {
                casillasOcupadas[i] = 0;
                celdasTablero.get(i).idImagenBarco = 0;
                celdasTablero.get(i).seleccionadaParaArma = false;
            }
        }
        armasEquipadasPorBarco.remove(id);

        actualizarEstadosBarcos();
        adaptador.notifyDataSetChanged();
    }

    private void actualizarEstadosBarcos() {
        barco5Colocado = false; barco3Colocado = false; barco1Colocado = false;
        for (int id : casillasOcupadas) {
            if (id == 0) continue;
            int count = 0;
            for (int x : casillasOcupadas) if (x == id) count++;
            if (count == 5) barco5Colocado = true;
            if (count == 3) barco3Colocado = true;
            if (count == 1) barco1Colocado = true;
        }
        btnShip5.setVisibility(barco5Colocado ? View.GONE : View.VISIBLE);
        btnShip3.setVisibility(barco3Colocado ? View.GONE : View.VISIBLE);
        btnShip1.setVisibility(barco1Colocado ? View.GONE : View.VISIBLE);
    }

    private void avanzarFase() {
        if (faseActual == 1) {
            if (barco5Colocado && barco3Colocado && barco1Colocado) {
                faseActual = 2;
                layoutControlesColocacion.setVisibility(View.GONE);
                ((TextView)findViewById(R.id.tv_title)).setText("Equipa tus Armas");
                Toast.makeText(this, "Selecciona un barco para equipar", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Coloca todos los barcos", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (armasEquipadasPorBarco.size() < 3) {
                Toast.makeText(this, "Debes repartir las 3 armas en tus 3 barcos", Toast.LENGTH_SHORT).show();
                return;
            }

            System.out.println("Configuración de Armas lista para enviar: " + armasEquipadasPorBarco.toString());
            Toast.makeText(this, "Configuración completada con éxito", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void actualizarMarcadorBarco(int idBarco) {
        limpiarMarcadorBarco();
        for (int i = 0; i < 225; i++) {
            if (casillasOcupadas[i] == idBarco) {
                celdasTablero.get(i).seleccionadaParaArma = true;
            }
        }
        adaptador.notifyDataSetChanged();
    }

    private void limpiarMarcadorBarco() {
        for (int i = 0; i < 225; i++) {
            celdasTablero.get(i).seleccionadaParaArma = false;
        }
        idBarcoSeleccionadoParaArma = 0;
        adaptador.notifyDataSetChanged();
    }
}