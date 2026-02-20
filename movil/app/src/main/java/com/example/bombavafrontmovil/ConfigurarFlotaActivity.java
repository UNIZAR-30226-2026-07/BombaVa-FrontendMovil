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
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    
    public class ConfigurarFlotaActivity extends AppCompatActivity {
    
        private ConfigurarFlotaAdapter adaptador;
        private List<Integer> coloresCeldas;
        private int[] casillasOcupadas = new int[225];
    
        private Map<Integer, String> armasEquipadas = new HashMap<>();
    
        private boolean enHorizontal = true;
        private int tamanoSeleccionado = 0;
        private int idBarcoActual = 1;
        private int faseActual = 1;
        private int idBarcoSeleccionadoParaArma = 0;
        private int posicionArmaSeleccionada = -1;
    
        private boolean barco5Colocado = false, barco3Colocado = false, barco1Colocado = false;
    
        private LinearLayout layoutControlesColocacion, layoutControlesArmas;
        private Button btnConfirmar, btnTorpedo, btnShip5, btnShip3, btnShip1, btnRotar;

        private int colorAguaEnemiga, colorAguaNeutra, colorAguaAliada;
        private int colorSeleccion, colorBarco5, colorBarco3, colorBarco1;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_configurar_flota);
    
            // 3 zonas de tablero
            colorAguaEnemiga = Color.parseColor("#90CAF9");
            colorAguaNeutra = Color.parseColor("#4FC3F7");
            colorAguaAliada = ContextCompat.getColor(this, R.color.sea_blue);
    
            colorSeleccion = ContextCompat.getColor(this, R.color.selection_yellow);
            colorBarco5 = Color.parseColor("#4E342E");
            colorBarco3 = Color.parseColor("#795548");
            colorBarco1 = Color.parseColor("#A1887F");
    
            vincularVistas();
            configurarTablero();
            configurarListeners();
        }
    
        private void vincularVistas() {
            layoutControlesColocacion = findViewById(R.id.layout_controles);
            layoutControlesArmas = findViewById(R.id.layout_controles_armas);
            btnConfirmar = findViewById(R.id.btn_confirmar);
            btnTorpedo = findViewById(R.id.btn_torpedo);
            btnShip5 = findViewById(R.id.btn_ship_5x1);
            btnShip3 = findViewById(R.id.btn_ship_3x1);
            btnShip1 = findViewById(R.id.btn_ship_1x1);
            btnRotar = findViewById(R.id.btn_rotate);
    
            btnRotar.setText("Rotar (H)");
        }
    
        private void configurarTablero() {
            RecyclerView rvTablero = findViewById(R.id.rvBoard);
            coloresCeldas = new ArrayList<>();

            for (int i = 0; i < 225; i++) {
                int fila = i / 15;
                if (fila < 5) coloresCeldas.add(colorAguaEnemiga);
                else if (fila < 10) coloresCeldas.add(colorAguaNeutra);
                else coloresCeldas.add(colorAguaAliada);
            }
    
            rvTablero.setLayoutManager(new GridLayoutManager(this, 15));
            adaptador = new ConfigurarFlotaAdapter(coloresCeldas, this::manejarToqueCelda);
            rvTablero.setAdapter(adaptador);
        }
    
        private void configurarListeners() {
            btnShip5.setOnClickListener(v -> { tamanoSeleccionado = 5; Toast.makeText(this, "Barco 5x1 seleccionado", Toast.LENGTH_SHORT).show(); });
            btnShip3.setOnClickListener(v -> { tamanoSeleccionado = 3; Toast.makeText(this, "Barco 3x1 seleccionado", Toast.LENGTH_SHORT).show(); });
            btnShip1.setOnClickListener(v -> { tamanoSeleccionado = 1; Toast.makeText(this, "Barco 1x1 seleccionado", Toast.LENGTH_SHORT).show(); });
    

            btnRotar.setOnClickListener(v -> {
                enHorizontal = !enHorizontal;
                btnRotar.setText(enHorizontal ? "Rotar (H)" : "Rotar (V)");
            });
    
            findViewById(R.id.btn_ametralladora).setOnClickListener(v -> equiparArmaAlBarco("Ametralladora"));
            findViewById(R.id.btn_misil).setOnClickListener(v -> equiparArmaAlBarco("Misil"));
            btnTorpedo.setOnClickListener(v -> equiparArmaAlBarco("Torpedo"));
    
            btnConfirmar.setOnClickListener(v -> avanzarFase());
        }
    
        private void manejarToqueCelda(int pos) {
            if (faseActual == 2) {
                int idBarco = casillasOcupadas[pos];
                if (idBarco != 0) {
                    List<Integer> celdas = obtenerCeldasDeBarco(idBarco);
    
                    if (celdas.size() == 1) {
                        limpiarMarcadorArma();
                        Toast.makeText(this, "El barco explorador no admite armas", Toast.LENGTH_SHORT).show();
                        return;
                    }
    
                    int motorPos = celdas.get(celdas.size() / 2);
    
                    if (pos == motorPos) {
                        limpiarMarcadorArma();
                        Toast.makeText(this, "Motor detectado: Selecciona un extremo", Toast.LENGTH_SHORT).show();
                        layoutControlesArmas.setVisibility(View.GONE);
                    } else {
                        actualizarMarcadorArma(pos);
                        idBarcoSeleccionadoParaArma = idBarco;
                        layoutControlesArmas.setVisibility(View.VISIBLE);
                        btnTorpedo.setVisibility(celdas.size() == 5 ? View.VISIBLE : View.GONE);
                    }
                } else {
                    limpiarMarcadorArma();
                    layoutControlesArmas.setVisibility(View.GONE);
                }
                return;
            }
    
            colocarBarco(pos);
        }
    
        private void colocarBarco(int pos) {

            if (casillasOcupadas[pos] != 0) {
                borrarBarco(casillasOcupadas[pos]);
                return;
            }
            if (tamanoSeleccionado == 0) {
                Toast.makeText(this, "Primero selecciona un barco de abajo", Toast.LENGTH_SHORT).show();
                return;
            }
    
            int fila = pos / 15;
            int col = pos % 15;

            // Si el barco se sale por la derecha, lo empujamos hacia la izquierda (se coloca por el punto central)
            if (enHorizontal && (col + tamanoSeleccionado > 15)) {
                col = 15 - tamanoSeleccionado;
                pos = (fila * 15) + col;
            }
            // Si el barco se sale por abajo, lo empujamos hacia arriba
            if (!enHorizontal && (fila + tamanoSeleccionado > 15)) {
                fila = 15 - tamanoSeleccionado;
                pos = (fila * 15) + col;
            }

    
            // Restricción de zonas (Solo permite colocar en filas 10 a 14)
            if (fila < 10) {
                Toast.makeText(this, "ZONA RESTRINGIDA: El barco no cabe y/o invade agua ajena", Toast.LENGTH_SHORT).show();
                return;
            }
    
            // Control de colisiones, de esta forma no superponemos barcos
            for (int i = 0; i < tamanoSeleccionado; i++) {
                int check = enHorizontal ? (pos + i) : (pos + (i * 15));
                if (casillasOcupadas[check] != 0) {
                    Toast.makeText(this, "Colisión: Hay otro barco en el camino", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
    
            int color = (tamanoSeleccionado == 5) ? colorBarco5 : (tamanoSeleccionado == 3 ? colorBarco3 : colorBarco1);
    
            for (int i = 0; i < tamanoSeleccionado; i++) {
                int p = enHorizontal ? (pos + i) : (pos + (i * 15));
                casillasOcupadas[p] = idBarcoActual;
                coloresCeldas.set(p, color);
            }
    
            // Marcamos como colocado y ocultamos el botón
            if (tamanoSeleccionado == 5) { barco5Colocado = true; btnShip5.setVisibility(View.GONE); }
            if (tamanoSeleccionado == 3) { barco3Colocado = true; btnShip3.setVisibility(View.GONE); }
            if (tamanoSeleccionado == 1) { barco1Colocado = true; btnShip1.setVisibility(View.GONE); }

            idBarcoActual++;
            tamanoSeleccionado = 0;
            adaptador.notifyDataSetChanged();
        }
    
        private void borrarBarco(int id) {
            for (int i = 0; i < 225; i++) {
                if (casillasOcupadas[i] == id) {
                    casillasOcupadas[i] = 0;

                    int fila = i / 15;
                    if (fila < 5) coloresCeldas.set(i, colorAguaEnemiga);
                    else if (fila < 10) coloresCeldas.set(i, colorAguaNeutra);
                    else coloresCeldas.set(i, colorAguaAliada);
                    armasEquipadas.remove(i);
                }
            }
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
    
            // Si el barco está colocado, se esconde el botón
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
                    Toast.makeText(this, "Toca los extremos de un barco", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Coloca todos los barcos", Toast.LENGTH_SHORT).show();
                }
            } else {
                // AQUÍ ES DONDE ESTÁ LA INFO PARA LA API (armasEquipadas)
                System.out.println("Configuración de Armas lista para enviar: " + armasEquipadas.toString());
                Toast.makeText(this, "Configuración completada y guardada", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    
        private List<Integer> obtenerCeldasDeBarco(int id) {
            List<Integer> celdas = new ArrayList<>();
            for (int i = 0; i < 225; i++) {
                if (casillasOcupadas[i] == id) celdas.add(i);
            }
            return celdas;
        }
    
        private void actualizarMarcadorArma(int nuevaPos) {
            if (posicionArmaSeleccionada != -1) restaurarColorBarco(posicionArmaSeleccionada);
            posicionArmaSeleccionada = nuevaPos;
            coloresCeldas.set(nuevaPos, colorSeleccion);
            adaptador.notifyDataSetChanged();
        }
    
        private void limpiarMarcadorArma() {
            if (posicionArmaSeleccionada != -1) restaurarColorBarco(posicionArmaSeleccionada);
            posicionArmaSeleccionada = -1;
            adaptador.notifyDataSetChanged();
        }
    
        private void restaurarColorBarco(int pos) {
            int id = casillasOcupadas[pos];
            if (id == 0) return;
            List<Integer> celdas = obtenerCeldasDeBarco(id);
            int color = (celdas.size() == 5) ? colorBarco5 : (celdas.size() == 3 ? colorBarco3 : colorBarco1);
            coloresCeldas.set(pos, color);
        }
    
        private void equiparArmaAlBarco(String tipo) {
            if (posicionArmaSeleccionada != -1) {
                // Guardamos el arma en una estructura para cuando usemos un api
                armasEquipadas.put(posicionArmaSeleccionada, tipo);
    
                Toast.makeText(this, tipo + " equipada en la celda " + posicionArmaSeleccionada, Toast.LENGTH_SHORT).show();
                restaurarColorBarco(posicionArmaSeleccionada);
                posicionArmaSeleccionada = -1;
                layoutControlesArmas.setVisibility(View.GONE);
                adaptador.notifyDataSetChanged();
            }
        }
    }