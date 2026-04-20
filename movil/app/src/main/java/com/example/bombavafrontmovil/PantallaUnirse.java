package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.bombavafrontmovil.network.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;

public class PantallaUnirse extends BaseActivity {

    private EditText etCodeInput;
    private Button btnJoin;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unirse);

        etCodeInput = findViewById(R.id.etCodeInput);
        btnJoin = findViewById(R.id.btnJoin);

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        SocketManager.getInstance().conectar(token);
        mSocket = SocketManager.getInstance().getSocket();

        configurarListenersSocket();

        btnJoin.setOnClickListener(v -> {
            String code = etCodeInput.getText().toString().trim().toUpperCase();

            if (code.length() >= 4) {
                try {
                    btnJoin.setEnabled(false);
                    btnJoin.setText("Uniendo...");

                    JSONObject payload = new JSONObject();
                    payload.put("codigo", code);
                    mSocket.emit("lobby:join", payload);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                AppNotifier.show(this, "Introduce un código válido", AppNotifier.Type.ERROR);
            }
        });
    }

    private void configurarListenersSocket() {
        mSocket.on("match:startInfo", args -> {
            try {
                GameStartCache.pendingStartInfo = (JSONObject) args[0];
            } catch (Exception ignored) {}
        });

        mSocket.on("match:ready", args -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String matchId = data.getString("matchId");

                    Intent intent = new Intent(PantallaUnirse.this, PantallaJuego.class);
                    intent.putExtra("MATCH_ID", matchId);
                    intent.putExtra("ES_HOST", false);
                    startActivity(intent);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        });

        mSocket.on("lobby:error", args -> runOnUiThread(() -> {
            try {
                JSONObject data = (JSONObject) args[0];
                AppNotifier.show(this, data.optString("message", "Error al unirse"), AppNotifier.Type.ERROR);
            } catch (Exception e) {
                AppNotifier.show(this, "Error al unirse", AppNotifier.Type.ERROR);
            }

            btnJoin.setEnabled(true);
            btnJoin.setText("Unirse");
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.off("match:startInfo");
            mSocket.off("match:ready");
            mSocket.off("lobby:error");
        }
    }
}