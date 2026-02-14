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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.juego);

        // Referencias
        layNoSel = findViewById(R.id.txtNoSelection);
        layMain = findViewById(R.id.layoutMainActions);
        layAtk = findViewById(R.id.layoutAttackActions);
        layMove = findViewById(R.id.layoutMoveActions);
        btnAtk2 = findViewById(R.id.btnAtk2);
        btnAtk3 = findViewById(R.id.btnAtk3);

        RecyclerView rv = findViewById(R.id.rvBoard);
        rv.setLayoutManager(new GridLayoutManager(this, 15));

        List<Casilla> matriz = new ArrayList<>();
        for (int i = 0; i < 225; i++) matriz.add(new Casilla(i / 15, i % 15));

        // CONFIGURACIÓN DE BARCOS DE PRUEBA
        matriz.get(20).setTipoBarco(1); matriz.get(20).setIdBarco(101);

        for(int i=50; i<=52; i++) { matriz.get(i).setTipoBarco(3); matriz.get(i).setIdBarco(102); }

        for(int i=120; i<=124; i++) { matriz.get(i).setTipoBarco(5); matriz.get(i).setIdBarco(103); }

        rv.setAdapter(new BoardAdapter(matriz, c -> {
            sel = c;
            mostrar(c.isTieneBarco() ? layMain : layNoSel);
        }));

        // BOTÓN INFO
        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            String info = "Nave Tipo " + sel.getTipoBarco() + "\nSalud: " + sel.getVidaActual() + "/" + sel.getVidaMax();
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        });

        // NAVEGACIÓN SUBMENÚS
        findViewById(R.id.btnMainAttack).setOnClickListener(v -> {
            btnAtk2.setVisibility(sel.getTipoBarco() >= 3 ? View.VISIBLE : View.GONE);
            btnAtk3.setVisibility(sel.getTipoBarco() == 5 ? View.VISIBLE : View.GONE);
            mostrar(layAtk);
        });

        findViewById(R.id.btnMainMove).setOnClickListener(v -> mostrar(layMove));

        // ACCIONES
        View.OnClickListener accion = v -> { Toast.makeText(this, "Ejecutando...", 0).show(); mostrar(layMain); };
        findViewById(R.id.btnAtk1).setOnClickListener(accion);
        findViewById(R.id.btnAtk2).setOnClickListener(accion);
        findViewById(R.id.btnAtk3).setOnClickListener(accion);
        findViewById(R.id.btnForward).setOnClickListener(accion);
        findViewById(R.id.btnBackward).setOnClickListener(accion);
        findViewById(R.id.btnRotateL).setOnClickListener(accion);
        findViewById(R.id.btnRotateR).setOnClickListener(accion);
    }

    private void mostrar(View v) {
        layNoSel.setVisibility(View.GONE); layMain.setVisibility(View.GONE);
        layAtk.setVisibility(View.GONE); layMove.setVisibility(View.GONE);
        v.setVisibility(View.VISIBLE);
    }
}