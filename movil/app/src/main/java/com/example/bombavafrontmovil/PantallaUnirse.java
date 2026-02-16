package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class PantallaUnirse extends BaseActivity {

    private EditText etCodeInput;
    private Button btnJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unirse);

        etCodeInput = findViewById(R.id.etCodeInput);
        btnJoin = findViewById(R.id.btnJoin);

        btnJoin.setOnClickListener(v -> {
            String code = etCodeInput.getText().toString().trim();

            if (code.length() == 4) {
                Toast.makeText(this, "¡Código " + code + " validado! Entrando...", Toast.LENGTH_SHORT).show();

                // Vamos al tablero de juego
                Intent intent = new Intent(PantallaUnirse.this, PantallaJuego.class);
                startActivity(intent);
                finish(); // Para que no pueda volver atrás al login de código
            } else {
                Toast.makeText(this, "El código debe tener 4 caracteres", Toast.LENGTH_SHORT).show();
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