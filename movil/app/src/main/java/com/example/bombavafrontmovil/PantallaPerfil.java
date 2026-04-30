package com.example.bombavafrontmovil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.models.RankingUser;
import com.example.bombavafrontmovil.models.User;
import com.example.bombavafrontmovil.network.ApiService;
import com.example.bombavafrontmovil.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PantallaPerfil extends AppCompatActivity {

    private TextView tvUsername, tvEmail, tvStatus;
    private Button btnSalaMando, btnCerrarSesion, btnRanking;
    private ImageButton btnEditar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // Enlazamos los elementos de la interfaz
        tvUsername = findViewById(R.id.tv_perfil_username);
        tvEmail = findViewById(R.id.tv_perfil_email);
        tvStatus = findViewById(R.id.tv_perfil_status);
        btnSalaMando = findViewById(R.id.btn_sala_mando);
        btnCerrarSesion = findViewById(R.id.btn_cerrar_sesion);
        btnEditar = findViewById(R.id.btnEditarPerfil);
        btnRanking = findViewById(R.id.btn_ranking);


        btnRanking.setOnClickListener(v -> mostrarDialogoRanking());

        // Configuramos el botón de la Sala de Mando
        btnSalaMando.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irASalaMando();
            }
        });

        // Configuramos el botón de Cerrar Sesión con el pop-up de confirmación
        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoCerrarSesion();
            }
        });

        // La primera vez que entramos, cargamos los datos
        cargarDatosDelUsuario();

        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(PantallaPerfil.this, EditarPerfilActivity.class);

            // Obtenemos el texto completo de los TextView
            String textoUsuario = tvUsername.getText().toString();
            String textoCorreo = tvEmail.getText().toString();

            // Limpiamos los textos
            String usuarioLimpio = textoUsuario.replace("USUARIO: ", "").trim();
            String correoLimpio = textoCorreo.replace("CORREO: ", "").replace("EMAIL: ", "").trim();

            // Pasamos los datos
            intent.putExtra("USUARIO_ACTUAL", usuarioLimpio);
            intent.putExtra("CORREO_ACTUAL", correoLimpio);

            startActivity(intent);
        });
    }

    // Este metodo se llama SIEMPRE que vuelves a esta pantalla
    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de EditarPerfilActivity, volvemos a descargar los datos actualizados
        cargarDatosDelUsuario();
    }

    private void cargarDatosDelUsuario() {
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        // Medida de seguridad: Si por algún motivo no hay token, lo mandamos al login
        if (token == null) {
            cerrarSesionDirecta();
            return;
        }

        ApiService api = RetrofitClient.getApiService();

        api.obtenerPerfil("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User usuario = response.body();

                    if (usuario.getUsername() != null) {
                        tvUsername.setText(usuario.getUsername().toUpperCase());
                    } else {
                        tvUsername.setText("USUARIO NO ENCONTRADO");
                    }
                    tvEmail.setText(usuario.getEmail() != null ? usuario.getEmail() : "Correo disponible...");
                    tvStatus.setText("Estado: LISTO PARA COMBATE");
                } else {
                    // Si el token ha caducado o el server da error 401/403
                    tvStatus.setText("Error: Autorización denegada.");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    AppNotifier.show(PantallaPerfil.this, "Sesión caducada. Vuelve a ingresar.", AppNotifier.Type.ERROR);
                    cerrarSesionDirecta();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                tvStatus.setText("Error de conexión. Modo offline.");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        });
    }

    private void irASalaMando() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void mostrarDialogoCerrarSesion() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_personalizado);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.setCancelable(true);

        android.widget.TextView tvTitulo = dialog.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMensaje = dialog.findViewById(R.id.tvDialogMessage);
        android.widget.Button btnConfirmar = dialog.findViewById(R.id.btnDialogAction);
        android.widget.Button btnCancelar = dialog.findViewById(R.id.btnDialogCancel);
        android.widget.ImageView ivIcono = dialog.findViewById(R.id.ivDialogIcon);

        // Configuración de textos para el Cierre de Sesión
        tvTitulo.setText("ABANDONAR FLOTA");
        tvMensaje.setText("¿Estás seguro de que deseas retirarte a puerto y cerrar sesión, Almirante?");
        ivIcono.setImageResource(android.R.drawable.ic_dialog_alert);
        ivIcono.setColorFilter(android.graphics.Color.parseColor("#5C3A21"));

        // Hacemos visible el botón de cancelar para este caso
        btnCancelar.setVisibility(android.view.View.VISIBLE);
        btnCancelar.setText("QUEDARSE");
        btnConfirmar.setText("SALIR");

        // Acción: Confirmar salida
        btnConfirmar.setOnClickListener(v -> {
            dialog.dismiss();
            cerrarSesionDirecta();
        });

        // Acción: Cancelar
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void cerrarSesionDirecta() {
        // Borramos el token guardado
        getSharedPreferences("BOMBA_VA", MODE_PRIVATE).edit().clear().apply();

        // Volvemos al Login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void mostrarDialogoRanking() {
        // Creamos el diálogo
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ranking);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        androidx.recyclerview.widget.RecyclerView rv = dialog.findViewById(R.id.rvRanking);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        Button btnClose = dialog.findViewById(R.id.btnCloseRanking);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Obtener el token
        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        // Llamada a la API
        ApiService apiService = RetrofitClient.getApiService();
        apiService.obtenerRanking("Bearer " + token).enqueue(new Callback<List<RankingUser>>() {
            @Override
            public void onResponse(Call<List<RankingUser>> call, Response<List<RankingUser>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RankingAdapter adapter = new RankingAdapter(response.body());
                    rv.setAdapter(adapter);
                } else {
                    // Opcional: Mostrar Toast de error si falla
                }
            }

            @Override
            public void onFailure(Call<List<RankingUser>> call, Throwable t) {
                // Opcional: Mostrar Toast de error de red
            }
        });
    }
}