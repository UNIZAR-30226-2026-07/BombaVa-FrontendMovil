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
        btnGoToGame.setEnabled(false);
        btnGoToGame.setText("Esperando rival...");

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        SocketManager.getInstance().conectar(token);
        mSocket = SocketManager.getInstance().getSocket();

        configurarListenersSocket();
        mSocket.emit("lobby:create");

        btnGoToGame.setOnClickListener(v ->
                Toast.makeText(this, "Espera a que se una un rival", Toast.LENGTH_SHORT).show()
        );
    }

    private void configurarListenersSocket() {
        mSocket.on("lobby:created", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                codigoGenerado = data.getString("codigo");
                tvGameCode.setText(codigoGenerado);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }));

        mSocket.on("match:startInfo", args -> {
            try {
                GameStartCache.pendingStartInfo = (JSONObject) args[0];
            } catch (Exception ignored) {}
        });

        mSocket.on("match:ready", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String matchId = data.getString("matchId");

                Intent intent = new Intent(LobbyActivity.this, PantallaJuego.class);
                intent.putExtra("MATCH_ID", matchId);
                intent.putExtra("CODIGO_SALA", codigoGenerado);
                intent.putExtra("ES_HOST", true);
                startActivity(intent);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }));

        mSocket.on("lobby:error", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Toast.makeText(this, data.optString("message", "Error de lobby"), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error de lobby", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.off("lobby:created");
            mSocket.off("match:startInfo");
            mSocket.off("match:ready");
            mSocket.off("lobby:error");
        }
    }
}