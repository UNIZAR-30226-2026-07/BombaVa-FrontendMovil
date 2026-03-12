package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

public class PantallaAjustes extends BaseActivity {

    private boolean esIngles = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        // Instanciamos SharedPreferences una sola vez para toda la clase
        prefs = getSharedPreferences("AjustesApp", MODE_PRIVATE);

        // 1. Referencias de la UI
        Button btnBack = findViewById(R.id.btnBackSettings);
        Button btnLanguage = findViewById(R.id.btnLanguage);
        Switch switchMusic = findViewById(R.id.switchMusic);
        Switch switchVibration = findViewById(R.id.switchVibration);
        SeekBar seekBarVolume = findViewById(R.id.seekBarVolume);

        // ==========================================
        // 2. CARGAR AJUSTES AL ABRIR LA PANTALLA
        // ==========================================

        // Idioma
        String currentLang = prefs.getString("idioma_preferido", "es");
        esIngles = currentLang.equals("en");
        btnLanguage.setText(esIngles ? "EN" : "ES");

        // Vibración (por defecto activada: true)
        switchVibration.setChecked(prefs.getBoolean("vibracion_activada", true));

        // Música (por defecto activada: true)
        switchMusic.setChecked(prefs.getBoolean("musica_activada", true));

        // Volumen (por defecto al 50%)
        seekBarVolume.setProgress(prefs.getInt("volumen_musica", 50));


        // ==========================================
        // 3. LISTENERS PARA GUARDAR LOS CAMBIOS
        // ==========================================

        // Listener Idioma (El que ya tenías)
        btnLanguage.setOnClickListener(v -> {
            String nuevoIdioma = esIngles ? "es" : "en";
            prefs.edit().putString("idioma_preferido", nuevoIdioma).apply();

            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                recreate();
            }
        });

        // Listener Vibración
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("vibracion_activada", isChecked).apply();
        });

        // Listener Música
        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("musica_activada", isChecked).apply();
            // TODO: Aquí luego podrías pausar o reanudar tu servicio de música directamente
        });

        // Listener Volumen
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Si quieres que el volumen cambie en tiempo real mientras arrastra, hazlo aquí
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int volumenActual = seekBar.getProgress();
                prefs.edit().putInt("volumen_musica", volumenActual).apply();
                Toast.makeText(PantallaAjustes.this, "Volumen guardado: " + volumenActual + "%", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón volver
        btnBack.setOnClickListener(v -> finish());
    }
}