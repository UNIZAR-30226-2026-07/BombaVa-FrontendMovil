package com.example.bombavafrontmovil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.bombavafrontmovil.models.*;
import com.example.bombavafrontmovil.network.ApiService;
import com.example.bombavafrontmovil.network.RetrofitClient;

public class EditarPerfilActivity extends AppCompatActivity {

    private EditText etEditUsername, etEditEmail;
    private Button btnGuardar, btnCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        etEditUsername = findViewById(R.id.etEditUsername);
        etEditEmail = findViewById(R.id.etEditEmail);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);
        btnCancelar = findViewById(R.id.btnCancelarPerfil);

        String usuarioActual = getIntent().getStringExtra("USUARIO_ACTUAL");
        String correoActual = getIntent().getStringExtra("CORREO_ACTUAL");

        if (usuarioActual != null) etEditUsername.setText(usuarioActual);
        if (correoActual != null) etEditEmail.setText(correoActual);

        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nuevoUsuario = etEditUsername.getText().toString().trim();
        String nuevoCorreo = etEditEmail.getText().toString().trim();

        boolean formularioValido = true;

        // Validaciones FrontEnd
        if (nuevoUsuario.isEmpty()) {
            etEditUsername.setError("El nombre de usuario no puede estar vacío");
            formularioValido = false;
        }

        if (nuevoCorreo.isEmpty()) {
            etEditEmail.setError("El correo no puede estar vacío");
            formularioValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(nuevoCorreo).matches()) {
            etEditEmail.setError("Introduce un correo electrónico válido");
            formularioValido = false;
        }

        if (!formularioValido) return;

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        HashMap<String, String> datos = new HashMap<>();
        datos.put("username", nuevoUsuario);
        datos.put("email", nuevoCorreo);

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", "");

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<Void> call = apiService.actualizarPerfil(token, datos);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", nuevoUsuario);
                    editor.putString("email", nuevoCorreo);
                    editor.apply();

                    mostrarDialogoEstilizado("¡Perfil actualizado!", "Los datos de tu expediente han sido modificados con éxito.", true, () -> finish());
                } else {
                    // Extraemos el error del backend (ej: "El usuario ya existe")
                    String errorMsg = "Error al actualizar el perfil";
                    try {
                        if (response.errorBody() != null) {
                            JSONObject jsonError = new JSONObject(response.errorBody().string());
                            if (jsonError.has("message")) errorMsg = jsonError.getString("message");
                        }
                    } catch (Exception e) { e.printStackTrace(); }

                    mostrarDialogoEstilizado("Modificación Denegada", errorMsg, false, null);
                    restaurarBoton();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mostrarDialogoEstilizado("Error de Conexión", "No se pudo contactar con el Cuartel General.", false, null);
                restaurarBoton();
            }
        });
    }

    private void restaurarBoton() {
        btnGuardar.setEnabled(true);
        btnGuardar.setText("Guardar Cambios");
    }

    // Pop-Up Personalizado para el Perfil
    private void mostrarDialogoEstilizado(String titulo, String mensaje, boolean exito, Runnable accionOk) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_personalizado);

        // Hacemos el marco exterior transparente para que solo se vea tu Pergamino
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.setCancelable(false);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnAccion = dialog.findViewById(R.id.btnDialogAction);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        tvTitulo.setText(titulo);
        tvMensaje.setText(mensaje);
        btnAccion.setText(exito ? "AVANZAR" : "ENTENDIDO");

        // Lógica estética adaptada al Pergamino
        if (exito) {
            ivIcono.setImageResource(R.drawable.ic_check_green);
            // Marrón oscuro original para éxitos
            tvTitulo.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            ivIcono.setColorFilter(android.graphics.Color.parseColor("#4CAF50"));
        } else {
            ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
            // Rojo oscuro (tipo lacre o sangre seca) para errores, encaja perfecto con madera/pergamino
            tvTitulo.setTextColor(android.graphics.Color.parseColor("#B71C1C"));
            ivIcono.setColorFilter(android.graphics.Color.parseColor("#B71C1C"));
        }

        btnAccion.setOnClickListener(v -> {
            dialog.dismiss();
            if (accionOk != null) accionOk.run();
        });

        dialog.show();
    }
}