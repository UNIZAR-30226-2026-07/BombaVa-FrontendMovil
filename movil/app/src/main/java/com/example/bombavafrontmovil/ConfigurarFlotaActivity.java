package com.example.bombavafrontmovil;

import android.graphics.Color;
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
import java.util.List;

public class ConfigurarFlotaActivity extends AppCompatActivity {

    private GestorConfiguracionFlota gestorLogica;
    private BotonesUIHelper uiHelper; // <-- NUEVO: Maneja los colores de los botones
    private ConfigurarFlotaAdapter adaptador;
    private List<CeldaVisual> celdasTablero;

    // Estados
    private boolean enHorizontal = true;
    private int tamanoSeleccionado = 0, faseActual = 1, idBarcoSeleccionadoParaArma = 0;
    private String armaTemporal = "";

    // Vistas contenedoras
    private LinearLayout layoutControlesColocacion, layoutControlesArmas;
    private Button btnRotar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_flota);

        gestorLogica = new GestorConfiguracionFlota();
        vincularVistasYListeners();
        configurarTablero();
    }

    private void vincularVistasYListeners() {
        layoutControlesColocacion = findViewById(R.id.layout_controles);
        layoutControlesArmas = findViewById(R.id.layout_controles_armas);
        btnRotar = findViewById(R.id.btn_rotate);

        Button btnShip5 = findViewById(R.id.btn_ship_5x1);
        Button btnShip3 = findViewById(R.id.btn_ship_3x1);
        Button btnShip1 = findViewById(R.id.btn_ship_1x1);
        Button btnAm = findViewById(R.id.btn_ametralladora);
        Button btnMi = findViewById(R.id.btn_misil);
        Button btnTo = findViewById(R.id.btn_torpedo);

        // Inicializamos el Helper que controlará los colores de estos botones
        uiHelper = new BotonesUIHelper(btnShip5, btnShip3, btnShip1, btnAm, btnMi, btnTo);

        btnShip5.setOnClickListener(v -> { tamanoSeleccionado = 5; uiHelper.resaltarBarco(5); });
        btnShip3.setOnClickListener(v -> { tamanoSeleccionado = 3; uiHelper.resaltarBarco(3); });
        btnShip1.setOnClickListener(v -> { tamanoSeleccionado = 1; uiHelper.resaltarBarco(1); });
        btnRotar.setOnClickListener(v -> { enHorizontal = !enHorizontal; btnRotar.setText(enHorizontal ? "Rotar (H)" : "Rotar (V)"); });

        btnAm.setOnClickListener(v -> seleccionarArmaTemporal("Ametralladora"));
        btnMi.setOnClickListener(v -> seleccionarArmaTemporal("Misil"));
        btnTo.setOnClickListener(v -> seleccionarArmaTemporal("Torpedo"));

        findViewById(R.id.btn_cancelar_arma).setOnClickListener(v -> cancelarSeleccionArma());
        findViewById(R.id.btn_guardar_arma).setOnClickListener(v -> guardarArmaBarco());
        findViewById(R.id.btn_confirmar).setOnClickListener(v -> avanzarFase());
    }

    private void configurarTablero() {
        RecyclerView rvTablero = findViewById(R.id.rvBoard);
        celdasTablero = new ArrayList<>();
        int colEnemiga = Color.parseColor("#90CAF9"), colNeutra = Color.parseColor("#4FC3F7"), colAliada = ContextCompat.getColor(this, R.color.sea_blue);

        for (int i = 0; i < 225; i++) {
            celdasTablero.add(new CeldaVisual((i / 15 < 5) ? colEnemiga : ((i / 15 < 10) ? colNeutra : colAliada)));
        }

        rvTablero.setHasFixedSize(true); rvTablero.setItemAnimator(null);
        rvTablero.setLayoutManager(new GridLayoutManager(this, 15));
        adaptador = new ConfigurarFlotaAdapter(celdasTablero, this::manejarToqueCelda);
        rvTablero.setAdapter(adaptador);
    }

    private void manejarToqueCelda(int pos) {
        int idBarco = gestorLogica.getIdBarcoEn(pos);
        if (faseActual == 2) {
            if (idBarco != 0) abrirPanelArmas(idBarco);
            else cancelarSeleccionArma();
        } else {
            intentarColocarOQuitarBarco(pos, idBarco);
        }
    }

    private void intentarColocarOQuitarBarco(int pos, int idBarcoExistente) {
        if (idBarcoExistente != 0) {
            if (tamanoSeleccionado == 0) borrarBarcoUI(idBarcoExistente);
            else Toast.makeText(this, "No puedes colocar un barco encima de otro", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tamanoSeleccionado == 0) return;

        int posAjustada = gestorLogica.ajustarPosicion(pos, tamanoSeleccionado, enHorizontal);
        int validacion = gestorLogica.validarColocacion(posAjustada, tamanoSeleccionado, enHorizontal);

        if (validacion == 1) Toast.makeText(this, "Zona inválida", Toast.LENGTH_SHORT).show();
        else if (validacion == 2) Toast.makeText(this, "Choca con otro barco", Toast.LENGTH_SHORT).show();
        else {
            gestorLogica.colocarBarco(posAjustada, tamanoSeleccionado, enHorizontal);
            dibujarBarcoEnVista(posAjustada);
            tamanoSeleccionado = 0;
            uiHelper.resaltarBarco(0);
            uiHelper.ocultarBarcosColocados(gestorLogica.estaBarcoColocado(5), gestorLogica.estaBarcoColocado(3), gestorLogica.estaBarcoColocado(1));
            adaptador.notifyDataSetChanged();
        }
    }

    private void dibujarBarcoEnVista(int posAjustada) {
        for (int i = 0; i < tamanoSeleccionado; i++) {
            CeldaVisual celda = celdasTablero.get(enHorizontal ? (posAjustada + i) : (posAjustada + (i * 15)));
            celda.rotacion = enHorizontal ? 90f : 0f;
            if (tamanoSeleccionado == 1) celda.idImagenBarco = R.drawable.barco_medio;
            else if (i == 0) celda.idImagenBarco = enHorizontal ? R.drawable.barco_popa : R.drawable.barco_proa;
            else if (i == tamanoSeleccionado - 1) celda.idImagenBarco = enHorizontal ? R.drawable.barco_proa : R.drawable.barco_popa;
            else celda.idImagenBarco = R.drawable.barco_medio;
        }
    }

    private void borrarBarcoUI(int idBarco) {
        for (int i = 0; i < 225; i++) {
            if (gestorLogica.getIdBarcoEn(i) == idBarco) {
                celdasTablero.get(i).idImagenBarco = 0; celdasTablero.get(i).seleccionadaParaArma = false;
            }
        }
        gestorLogica.borrarBarco(idBarco);
        uiHelper.ocultarBarcosColocados(gestorLogica.estaBarcoColocado(5), gestorLogica.estaBarcoColocado(3), gestorLogica.estaBarcoColocado(1));
        adaptador.notifyDataSetChanged();
    }

    private void abrirPanelArmas(int idBarco) {
        idBarcoSeleccionadoParaArma = idBarco;
        armaTemporal = "";
        actualizarMarcadorBarco(idBarco);

        // Averiguamos el tamaño del barco seleccionado
        int tamano = gestorLogica.getTamanoBarco(idBarco);

        // Le pasamos al Helper el tamaño y qué armas tiene ya puestas
        uiHelper.actualizarBotonesArmas(
                tamano,
                gestorLogica.tieneArmaEquipada(idBarco, "Ametralladora"),
                gestorLogica.tieneArmaEquipada(idBarco, "Misil"),
                gestorLogica.tieneArmaEquipada(idBarco, "Torpedo")
        );

        uiHelper.resaltarArma("");
        layoutControlesArmas.setVisibility(View.VISIBLE);
    }

    private void seleccionarArmaTemporal(String tipo) {
        armaTemporal = tipo;
        uiHelper.resaltarArma(tipo);
    }

    private void guardarArmaBarco() {
        if (idBarcoSeleccionadoParaArma != 0 && !armaTemporal.isEmpty()) {
            gestorLogica.equiparArma(idBarcoSeleccionadoParaArma, armaTemporal);
            Toast.makeText(this, armaTemporal + " equipada!", Toast.LENGTH_SHORT).show();

            // MAGIA: En lugar de cerrar el panel, lo "refrescamos".
            // Así el botón del arma que acabamos de equipar desaparecerá mágicamente
            // y podremos equipar la siguiente sin volver a tocar el barco.
            abrirPanelArmas(idBarcoSeleccionadoParaArma);
        } else {
            Toast.makeText(this, "Selecciona un arma primero", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelarSeleccionArma() {
        actualizarMarcadorBarco(0);
        armaTemporal = "";
        layoutControlesArmas.setVisibility(View.GONE);
    }

    private void actualizarMarcadorBarco(int idBarco) {
        for (int i = 0; i < 225; i++) celdasTablero.get(i).seleccionadaParaArma = (idBarco != 0 && gestorLogica.getIdBarcoEn(i) == idBarco);
        idBarcoSeleccionadoParaArma = idBarco;
        adaptador.notifyDataSetChanged();
    }

    private void avanzarFase() {
        if (faseActual == 1) {
            // Fase 1: Sigue siendo obligatorio colocar los 3 barcos
            if (gestorLogica.estaBarcoColocado(5) && gestorLogica.estaBarcoColocado(3) && gestorLogica.estaBarcoColocado(1)) {
                faseActual = 2;
                layoutControlesColocacion.setVisibility(View.GONE);
                ((TextView)findViewById(R.id.tv_title)).setText("Equipa tus Armas");
                // Le damos una pista al jugador de que esto es opcional
                Toast.makeText(this, "Equipa tus armas (opcional) y pulsa Confirmar", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Coloca todos los barcos", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Fase 2: Como las armas son opcionales, simplemente terminamos la Activity y guardamos
            Toast.makeText(this, "¡Flota lista para el combate!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}