package com.example.bombavafrontmovil;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PantallaJuego extends AppCompatActivity {
    private View layNoSel, layMain, layAtk, layMove;
    private Button btnAtk2, btnAtk3;
    private Casilla sel = null;
    private BoardAdapter adapter;

    // MATRIZ Y LISTA DE BARCOS
    private List<Casilla> matriz = new ArrayList<>();
    private List<BarcoLogico> flota = new ArrayList<>();

    // CLASE INTERNA PARA LA LÓGICA ESPACIAL DEL BARCO
    class BarcoLogico {
        int id, tipo, fCentro, cCentro, dir, vidaActual;
        // dir: 0=Norte, 1=Este, 2=Sur, 3=Oeste

        public BarcoLogico(int id, int tipo, int fCentro, int cCentro, int dir) {
            this.id = id; this.tipo = tipo; this.fCentro = fCentro;
            this.cCentro = cCentro; this.dir = dir; this.vidaActual = tipo;
        }

        public BarcoLogico clonar() { return new BarcoLogico(id, tipo, fCentro, cCentro, dir); }

        // Devuelve una lista de las coordenadas [fila, columna, esProa(1 o 0)]
        public List<int[]> getCeldas() {
            List<int[]> celdas = new ArrayList<>();
            int df = 0, dc = 0;
            if(dir == 0) df = -1; else if(dir == 1) dc = 1; else if(dir == 2) df = 1; else if(dir == 3) dc = -1;

            int offset = tipo / 2; // Distancia del centro a las puntas
            for (int i = -offset; i <= offset; i++) {
                boolean esProa = (i == offset && tipo > 1); // El de tamaño 1 no necesita marcar proa
                celdas.add(new int[]{fCentro + (df * i), cCentro + (dc * i), esProa ? 1 : 0});
            }
            return celdas;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        layNoSel = findViewById(R.id.txtNoSelection);
        layMain = findViewById(R.id.layoutMainActions);
        layAtk = findViewById(R.id.layoutAttackActions);
        layMove = findViewById(R.id.layoutMoveActions);
        btnAtk2 = findViewById(R.id.btnAtk2);
        btnAtk3 = findViewById(R.id.btnAtk3);

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));

        // Inicializar matriz vacía
        for (int i = 0; i < 225; i++) matriz.add(new Casilla(i / 15, i % 15));

        // INICIALIZAR FLOTA CON LÓGICA (ID, Tamaño, Fila, Columna, Dirección)
        flota.add(new BarcoLogico(101, 1, 2, 2, 0)); // Pequeño
        flota.add(new BarcoLogico(102, 3, 5, 5, 1)); // Mediano mirando al ESTE
        flota.add(new BarcoLogico(103, 5, 10, 8, 0)); // Grande mirando al NORTE

        actualizarMatrizVisual(); // Pinta los barcos en la cuadrícula

        adapter = new BoardAdapter(matriz, c -> {
            sel = c;
            mostrar(c.isTieneBarco() ? layMain : layNoSel);
        });
        rv.setAdapter(adapter);

        // BOTÓN INFO
        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            if (sel != null && sel.isTieneBarco()) {
                Toast.makeText(this, "Nave Tipo " + sel.getTipoBarco() + "\nSalud: " + sel.getVidaActual() + "/" + sel.getVidaMax(), Toast.LENGTH_SHORT).show();
            }
        });

        // NAVEGACIÓN SUBMENÚS
        findViewById(R.id.btnMainAttack).setOnClickListener(v -> {
            btnAtk2.setVisibility(sel.getTipoBarco() >= 3 ? View.VISIBLE : View.GONE);
            btnAtk3.setVisibility(sel.getTipoBarco() == 5 ? View.VISIBLE : View.GONE);
            mostrar(layAtk);
        });
        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));

        // LISTENER DE MOVIMIENTO
        View.OnClickListener listenerMov = v -> {
            boolean exito = false;
            if (v.getId() == R.id.btnForward) exito = intentarMovimiento(1);
            else if (v.getId() == R.id.btnBackward) exito = intentarMovimiento(2);
            else if (v.getId() == R.id.btnRotateL) exito = intentarMovimiento(3);
            else if (v.getId() == R.id.btnRotateR) exito = intentarMovimiento(4);

            if (exito) Toast.makeText(this, "Movimiento exitoso", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "No se puede mover ahí (fuera del mapa o choque)", Toast.LENGTH_SHORT).show();

            mostrar(layMain);
        };

        findViewById(R.id.btnForward).setOnClickListener(listenerMov);
        findViewById(R.id.btnBackward).setOnClickListener(listenerMov);
        findViewById(R.id.btnRotateL).setOnClickListener(listenerMov);
        findViewById(R.id.btnRotateR).setOnClickListener(listenerMov);

        // ATAQUES
        View.OnClickListener accionAtk = v -> { Toast.makeText(this, "¡Fuego!", Toast.LENGTH_SHORT).show(); mostrar(layMain); };
        findViewById(R.id.btnAtk1).setOnClickListener(accionAtk);
        findViewById(R.id.btnAtk2).setOnClickListener(accionAtk);
        findViewById(R.id.btnAtk3).setOnClickListener(accionAtk);
    }

    // --- MÉTODOS DE LÓGICA DE MOVIMIENTO ---

    private boolean intentarMovimiento(int accion) {
        if (sel == null || !sel.isTieneBarco()) return false;

        // 1. Encontrar el barco seleccionado en la lista
        BarcoLogico barcoReal = null;
        for (BarcoLogico b : flota) {
            if (b.id == sel.getIdBarco()) { barcoReal = b; break; }
        }
        if (barcoReal == null) return false;

        // 2. Crear un clon para probar el movimiento antes de aplicarlo
        BarcoLogico clon = barcoReal.clonar();
        int df = 0, dc = 0;
        if(clon.dir == 0) df = -1; else if(clon.dir == 1) dc = 1; else if(clon.dir == 2) df = 1; else if(clon.dir == 3) dc = -1;

        switch (accion) {
            case 1: clon.fCentro += df; clon.cCentro += dc; break; // Adelante
            case 2: clon.fCentro -= df; clon.cCentro -= dc; break; // Atrás
            case 3: clon.dir = (clon.dir + 3) % 4; break; // Rotar Izquierda (-90º)
            case 4: clon.dir = (clon.dir + 1) % 4; break; // Rotar Derecha (+90º)
        }

        // 3. Validar que el clon no choca ni se sale del mapa
        for (int[] celda : clon.getCeldas()) {
            int f = celda[0], c = celda[1];
            if (f < 0 || f >= 15 || c < 0 || c >= 15) return false; // Se sale

            Casilla casillaDestino = matriz.get(f * 15 + c);
            if (casillaDestino.isTieneBarco() && casillaDestino.getIdBarco() != clon.id) {
                return false; // Choca con otro barco
            }
        }

        // 4. Si es válido, aplicar al barco real y repintar
        barcoReal.fCentro = clon.fCentro;
        barcoReal.cCentro = clon.cCentro;
        barcoReal.dir = clon.dir;

        actualizarMatrizVisual();
        if(adapter != null) adapter.notifyDataSetChanged();
        return true;
    }

    private void actualizarMatrizVisual() {
        int idSeleccionado = (sel != null && sel.isTieneBarco()) ? sel.getIdBarco() : -1;

        // Limpiar la matriz por completo
        for (Casilla c : matriz) {
            c.setTipoBarco(0); c.setIdBarco(-1); c.setEsProa(false); c.setSeleccionado(false);
        }

        // Repintar todos los barcos en sus nuevas coordenadas
        for (BarcoLogico b : flota) {
            for (int[] celda : b.getCeldas()) {
                int f = celda[0], c = celda[1];
                if (f >= 0 && f < 15 && c >= 0 && c < 15) {
                    Casilla casilla = matriz.get(f * 15 + c);
                    casilla.setTipoBarco(b.tipo);
                    casilla.setIdBarco(b.id);
                    casilla.setEsProa(celda[2] == 1);
                    casilla.setVidaActual(b.vidaActual);

                    // Mantener la selección visual tras el movimiento
                    if (b.id == idSeleccionado) {
                        casilla.setSeleccionado(true);
                        sel = casilla; // Actualizamos la referencia
                    }
                }
            }
        }
    }

    private void mostrar(View v) {
        if(layNoSel != null) layNoSel.setVisibility(View.GONE);
        if(layMain != null) layMain.setVisibility(View.GONE);
        if(layAtk != null) layAtk.setVisibility(View.GONE);
        if(layMove != null) layMove.setVisibility(View.GONE);
        v.setVisibility(View.VISIBLE);
    }
}