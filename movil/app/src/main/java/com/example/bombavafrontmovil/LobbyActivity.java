package com.example.bombavafrontmovil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class LobbyActivity extends AppCompatActivity {

    private TextView tvGameCode;
    private Button btnGoToGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        tvGameCode = findViewById(R.id.tvGameCode);
        btnGoToGame = findViewById(R.id.btnGoToGame);


        String randomCode = generateRandomCode();
        tvGameCode.setText(randomCode);

        // Botón para avanzar al juego (Debug)
        btnGoToGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LobbyActivity.this, PantallaJuego.class);
                startActivity(intent);
            }
        });
    }

    // Método auxiliar para generar código de 4 letras/números
    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}