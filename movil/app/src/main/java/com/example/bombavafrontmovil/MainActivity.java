package com.example.bombavafrontmovil;

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

        // --- BOTÓN COMPETITIVO ---
        if (btnCompetitivo != null) {
            btnCompetitivo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                        String userId = prefs.getString("userId", "default");
                        boolean tieneMazo = prefs.getBoolean("flota_guardada_" + userId, false);

                        if (tieneMazo) {
                            startActivity(new Intent(MainActivity.this, PantallaJuego.class));
                        } else {
                            mostrarAvisoMazoVacio();
                        }
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }

        // --- BOTÓN UNIRSE ---
        if (btnUnirse != null) {
            btnUnirse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                        String userId = prefs.getString("userId", "default");
                        boolean tieneMazo = prefs.getBoolean("flota_guardada_" + userId, false);

                        if (tieneMazo) {
                            startActivity(new Intent(MainActivity.this, PantallaUnirse.class));
                        } else {
                            mostrarAvisoMazoVacio();
                        }
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }

        // --- BOTÓN PRÁCTICA ---
        if (btnPractica != null) {
            btnPractica.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isUsuarioLogueado()) {
                        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
                        String userId = prefs.getString("userId", "default");
                        boolean tieneMazo = prefs.getBoolean("flota_guardada_" + userId, false);

                        if (tieneMazo) {
                            startActivity(new Intent(MainActivity.this, LobbyActivity.class));
                        } else {
                            mostrarAvisoMazoVacio();
                        }
                    } else {
                        mostrarDialogoAlistamiento();
                    }
                }
            });
        }

        // --- BOTÓN CONFIGURAR FLOTA ---
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

        // --- BOTÓN PERFIL ---
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

        // --- BOTÓN AJUSTES ---
        if (btnAjustes != null) {
            btnAjustes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navegamos directamente a la pantalla de Ajustes
                    startActivity(new Intent(MainActivity.this, PantallaAjustes.class));
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

    // Pop-up estilizado con pergamino y madera (Login)
    private void mostrarDialogoAlistamiento() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado);

        // Hacemos el marco exterior transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.setCancelable(true);

        // Enlazamos las vistas
        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnIdentificarse = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        // Configuración de textos y colores para advertencia
        tvTitulo.setText("¡ACCESO RESTRINGIDO!");
        tvMensaje.setText("Atención: Necesitas estar alistado en la flota para acceder a esta sección de la Sala de Mando.\n\n¿Deseas identificarte o crear una cuenta ahora?");

        // Ponemos el icono de alerta y le damos el tono rojo oscuro/borgoña
        ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#B71C1C"));
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#B71C1C"));

        // Hacemos visible el botón secundario
        btnCancelar.setVisibility(android.view.View.VISIBLE);

        btnCancelar.setText("PERMANECER");
        btnIdentificarse.setText("IDENTIFICARSE");

        // Ir al Login
        btnIdentificarse.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Quedarse en la pantalla actual
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Pop-up para advertir que NO tienen mazo configurado
    private void mostrarAvisoMazoVacio() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_personalizado); // Reutilizamos tu diseño base

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.setCancelable(true);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnIrAConfigurar = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText("¡FLOTA NO PREPARADA!");
        tvMensaje.setText("Comandante, no puedes ir a la batalla sin tus barcos. Por favor, dirígete a los astilleros y configura tu flota antes de buscar partida.");

        ivIcono.setImageResource(android.R.drawable.ic_dialog_info);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#1976D2")); // Color Azul Marino
        tvTitulo.setTextColor(android.graphics.Color.parseColor("#1976D2"));

        btnCancelar.setVisibility(View.VISIBLE);
        btnCancelar.setText("MÁS TARDE");
        btnIrAConfigurar.setText("CONFIGURAR FLOTA");

        // Ir directo a Configurar Flota
        btnIrAConfigurar.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, ConfigurarFlotaActivity.class));
        });

        // Quedarse en la pantalla actual
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}