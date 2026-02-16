package com.example.bombavafrontmovil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

public class PantallaAjustes extends BaseActivity {

    private boolean esIngles = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        // 1. Referencias de la UI
        Button btnBack = findViewById(R.id.btnBackSettings);
        Button btnLanguage = findViewById(R.id.btnLanguage);
        Switch switchMusic = findViewById(R.id.switchMusic);
        Switch switchVibration = findViewById(R.id.switchVibration);
        SeekBar seekBarVolume = findViewById(R.id.seekBarVolume);

        // 2. Comprobar idioma actual para configurar el estado inicial
        String currentLang = getSharedPreferences("AjustesApp", MODE_PRIVATE)
                .getString("idioma_preferido", "es");

        esIngles = currentLang.equals("en");
        btnLanguage.setText(esIngles ? "EN" : "ES");

        // 3. UN SOLO Listener para el botón de idioma
        btnLanguage.setOnClickListener(v -> {
            // Cambiamos el valor
            String nuevoIdioma = esIngles ? "es" : "en";

            // Guardamos en SharedPreferences
            getSharedPreferences("AjustesApp", MODE_PRIVATE)
                    .edit()
                    .putString("idioma_preferido", nuevoIdioma)
                    .apply();

            // REINICIO DE LA APP:
            // Intentamos obtener el intent de lanzamiento de forma segura
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());

            if (intent != null) {
                // Limpiamos toda la pila de pantallas para que no queden pantallas en el idioma viejo
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Cerramos la pantalla actual de ajustes
            } else {
                // Si por alguna razón falla el reinicio total, usamos el método simple
                recreate();
            }
        });

        // 4. Botón volver
        btnBack.setOnClickListener(v -> finish());

        // 5. Lógica del SeekBar
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(PantallaAjustes.this, "Volumen: " + seekBar.getProgress() + "%", Toast.LENGTH_SHORT).show();
            }
        });
    }
}