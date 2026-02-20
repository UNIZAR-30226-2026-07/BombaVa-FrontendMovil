package com.example.bombavafrontmovil;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // Importante
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnConfigFlota, btnCompetitivo, btnPractica, btnUnirse;
    private ImageButton btnSettings, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        btnCompetitivo = findViewById(R.id.btnCompetitivo);
        btnPractica = findViewById(R.id.btnPractica);
        btnUnirse = findViewById(R.id.btnUnirse);
        btnConfigFlota = findViewById(R.id.btnConfigFlota);

        btnConfigFlota.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConfigurarFlotaActivity.class);
            startActivity(intent);
        });

        btnCompetitivo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
            startActivity(intent);
        });

        btnUnirse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PantallaUnirse.class);
                startActivity(intent);
            }
        });

        btnPractica.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PantallaJuego.class);
            startActivity(intent);
        });

        ImageButton btnProfile = findViewById(R.id.btnProfile);


        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PantallaPerfil.class);
                startActivity(intent);
            }
        });

        ImageButton btnSettings = findViewById(R.id.btnSettings);


        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PantallaAjustes.class);
                startActivity(intent);
            }
        });
    }

    private void mostrarDialogoUnirsePartida() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unirse a partida");
        builder.setMessage("Introduce el código de la sala (4 caracteres):");

        // Configurar el campo de texto (input)
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        builder.setView(input);

        // Botón ACEPTAR
        builder.setPositiveButton("Entrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String codigo = input.getText().toString().toUpperCase();

                // Validación simple (simulamos que "si tiene 4 letras, existe")
                if (codigo.length() == 4) {
                    // CÓDIGO VÁLIDO -> Vamos al juego
                    Intent intent = new Intent(MainActivity.this, PantallaJuego.class);
                    // Opcional: Pasamos el código a la siguiente pantalla por si hace falta
                    intent.putExtra("CODIGO_SALA", codigo);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "El código debe tener 4 caracteres", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Botón CANCELAR
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}