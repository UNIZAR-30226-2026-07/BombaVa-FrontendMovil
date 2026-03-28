package com.example.bombavafrontmovil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private View btnCompetitivo, btnUnirse, btnConfigurarFlota, btnPerfil, btnAjustes, btnPractica;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enlazamos las vistas
        btnCompetitivo = findViewById(R.id.btnCompetitivo);
        btnUnirse = findViewById(R.id.btnUnirse);
        btnConfigurarFlota = findViewById(R.id.btnConfigFlota);
        btnPerfil = findViewById(R.id.btnProfile);
        btnPractica = findViewById(R.id.btnPractica);
        btnAjustes = findViewById(R.id.btnSettings);

        // Zonas restringidas (requieren login)
        if (btnCompetitivo != null) {
            btnCompetitivo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        startActivity(new Intent(MainActivity.this, PantallaJuego.class));
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }

        if (btnUnirse != null) {
            btnUnirse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        startActivity(new Intent(MainActivity.this, PantallaUnirse.class));
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }

        if (btnConfigurarFlota != null) {
            btnConfigurarFlota.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        startActivity(new Intent(MainActivity.this, ConfigurarFlotaActivity.class));
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }

        // Botón de Perfil (Login o Perfil según el estado)
        if (btnPerfil != null) {
            btnPerfil.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        startActivity(new Intent(MainActivity.this, PantallaPerfil.class));
                    } else {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    }
                }
            });
        }

        if (btnAjustes != null) {
            btnAjustes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navegamos directamente a la pantalla de Ajustes
                    startActivity(new Intent(MainActivity.this, PantallaAjustes.class));
                }
            });
        }

        if (btnPractica != null) {
            btnPractica.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        startActivity(new Intent(MainActivity.this, LobbyActivity.class));
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }
    }

    // --- MÉTODOS DE SEGURIDAD NAVAL ---

    private boolean isUsuarioLogueado() {
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        return token != null && !token.isEmpty();
    }

    private void mostrarDialogoAlistamiento() {
        new AlertDialog.Builder(this)
                .setTitle("¡Acceso Restringido!")
                .setMessage("Atención: Necesitas estar alistado en la flota para acceder a esta sección de la Sala de Mando.\n\n¿Deseas identificarte o crear una cuenta ahora?")
                .setPositiveButton("Identificarse", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Permanecer en cubierta", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(true)
                .show();
    }
}