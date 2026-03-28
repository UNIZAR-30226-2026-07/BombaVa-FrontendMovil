package com.example.bombavafrontmovil;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.bombavafrontmovil.models.*;
import com.example.bombavafrontmovil.network.ApiClient;
import com.example.bombavafrontmovil.network.ApiService;
import com.example.bombavafrontmovil.network.RetrofitClient;

public class EditarPerfilActivity extends AppCompatActivity {

    private EditText etEditUsername, etEditEmail;
    private Button btnGuardar, btnCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Enlazamos variables
        etEditUsername = findViewById(R.id.etEditUsername);
        etEditEmail = findViewById(R.id.etEditEmail);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);
        btnCancelar = findViewById(R.id.btnCancelarPerfil);

        // Recuperar datos de la pantalla anterior
        String usuarioActual = getIntent().getStringExtra("USUARIO_ACTUAL");
        String correoActual = getIntent().getStringExtra("CORREO_ACTUAL");

        // Escribir en las cajas el contenido
        if (usuarioActual != null) {
            etEditUsername.setText(usuarioActual);
        }
        if (correoActual != null) {
            etEditEmail.setText(correoActual);
        }

        // Configurar botones
        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nuevoUsuario = etEditUsername.getText().toString().trim();
        String nuevoCorreo = etEditEmail.getText().toString().trim();

        if (nuevoUsuario.isEmpty() || nuevoCorreo.isEmpty()) {
            Toast.makeText(this, "Por favor, rellena ambos campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        // Preparar los datos
        HashMap<String, String> datos = new HashMap<>();
        datos.put("username", nuevoUsuario);
        datos.put("email", nuevoCorreo);

        // Recuperar Token
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        // Hacer la llamada a Retrofit (Ajusta 'RetrofitClient.getApiService()' a tu código real)
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<Void> call = apiService.actualizarPerfil(token, datos);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditarPerfilActivity.this, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show();

                    // Actualizamos también las preferencias locales por si las usas en otras pantallas
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", nuevoUsuario);
                    editor.putString("email", nuevoCorreo);
                    editor.apply();

                    finish(); // Cerramos esta pantalla y volvemos al Perfil
                } else {
                    Toast.makeText(EditarPerfilActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar Cambios");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditarPerfilActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                btnGuardar.setEnabled(true);
                btnGuardar.setText("Guardar Cambios");
            }
        });
    }
}