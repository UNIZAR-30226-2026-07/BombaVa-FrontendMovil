package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.network.SocketManager;
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.Socket;

public class LobbyActivity extends AppCompatActivity {

    private TextView tvGameCode;
    private Button btnGoToGame;
    private Socket mSocket;
    private String codigoGenerado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        tvGameCode = findViewById(R.id.tvGameCode);
        btnGoToGame = findViewById(R.id.btnGoToGame);

        tvGameCode.setText("Generando...");
        btnGoToGame.setEnabled(false); // Deshabilitado hasta que el servidor nos dé un código

        // Conectar el Socket
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        SocketManager.getInstance().conectar(token);
        mSocket = SocketManager.getInstance().getSocket();

        // Escuchar la creación de la sala
        mSocket.on("lobby:created", args -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    codigoGenerado = data.getString("codigo");
                    tvGameCode.setText(codigoGenerado);
                    btnGoToGame.setEnabled(true); // Ya podemos ir al tablero
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        });

        // Pedir al servidor que cree la sala
        mSocket.emit("lobby:create");

        // Botón para ir al tablero
        btnGoToGame.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyActivity.this, PantallaJuego.class);
            intent.putExtra("CODIGO_SALA", codigoGenerado);
            intent.putExtra("ES_HOST", true); // Indicamos que somos los creadores
            startActivity(intent);
            finish(); // Cerramos el lobby
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Quitamos la escucha al salir
        if (mSocket != null) {
            mSocket.off("lobby:created");
        }
    }
}