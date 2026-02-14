package com.example.bombavafrontmovil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicamos el idioma antes de que la actividad haga nada más
        aplicarIdiomaConfig();
        super.onCreate(savedInstanceState);
    }

    private void aplicarIdiomaConfig() {
        SharedPreferences prefs = getSharedPreferences("AjustesApp", MODE_PRIVATE);
        String lang = prefs.getString("idioma_preferido", "es");

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        // Esto hace que la actividad actual use el idioma guardado
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}