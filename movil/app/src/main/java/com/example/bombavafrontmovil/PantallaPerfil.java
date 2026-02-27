package com.example.bombavafrontmovil;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class PantallaPerfil extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        Button btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Al cerrar sesión, lo enviamos de vuelta al MainActivity
                Intent intent = new Intent(PantallaPerfil.this, MainActivity.class);
                // Estas "flags" limpian el historial para que no pueda volver al perfil dándole "Atrás"
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
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