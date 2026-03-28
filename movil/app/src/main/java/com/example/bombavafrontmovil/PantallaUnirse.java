package com.example.bombavafrontmovil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Locale;

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

        // Conectar el Socket
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        SocketManager.getInstance().conectar(token);
        mSocket = SocketManager.getInstance().getSocket();

        // Configurar las escuchas
        configurarListenersSocket();

        // Acción del botón
        btnJoin.setOnClickListener(v -> {
            String code = etCodeInput.getText().toString().trim().toUpperCase();

            // Tu validación original
            if (code.length() >= 4) { // >= por si hay codigo mas largos
                try {
                    btnJoin.setEnabled(false);
                    btnJoin.setText("Uniendo...");

                    // Enviar la petición al servidor
                    JSONObject payload = new JSONObject();
                    payload.put("codigo", code);
                    mSocket.emit("lobby:join", payload);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Introduce un código válido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarListenersSocket() {
        // Cuando el servidor confirma que nos hemos unido bien y empieza la partida
        mSocket.on("match:ready", args -> {
            runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String matchId = data.getString("matchId");

                    Toast.makeText(this, "¡Unido con éxito!", Toast.LENGTH_SHORT).show();

                    // Saltamos a PantallaJuego pasándole el ID real de la partida
                    Intent intent = new Intent(PantallaUnirse.this, PantallaJuego.class);
                    intent.putExtra("MATCH_ID", matchId);
                    startActivity(intent);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        });

        // Si el código está mal, la sala no existe o está llena
        mSocket.on("lobby:error", args -> {
            runOnUiThread(() -> {
                btnJoin.setEnabled(true);
                btnJoin.setText("Unirse"); // Restaurar el botón
                try {
                    JSONObject error = (JSONObject) args[0];
                    String mensaje = error.optString("message", "Código inválido o sala llena");
                    Toast.makeText(this, "Error: " + mensaje, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("SOCKET", "Error al leer lobby:error", e);
                }
            });
        });
    }

    private void aplicarIdiomaGuardado() {
        // Tu lógica original de idiomas se queda intacta
        String lang = getSharedPreferences("AjustesApp", MODE_PRIVATE)
                .getString("idioma_preferido", "es");

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar listeners
        if (mSocket != null) {
            mSocket.off("match:ready");
            mSocket.off("lobby:error");
        }
    }
}