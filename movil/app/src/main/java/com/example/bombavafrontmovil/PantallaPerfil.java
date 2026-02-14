package com.example.bombavafrontmovil;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class PantallaPerfil extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        Button btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            // Simulación de cerrar sesión
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
            finish(); // Cierra esta pantalla y vuelve al menú
        });
    }

    private void aplicarIdiomaGuardado() {
        // Leer el idioma guardado (por defecto español "es")
        String lang = getSharedPreferences("AjustesApp", MODE_PRIVATE)
                .getString("idioma_preferido", "es");

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}