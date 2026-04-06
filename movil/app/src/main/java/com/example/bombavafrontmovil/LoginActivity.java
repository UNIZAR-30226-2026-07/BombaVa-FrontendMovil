package com.example.bombavafrontmovil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.models.AuthResponse;
import com.example.bombavafrontmovil.models.LoginRequest;
import com.example.bombavafrontmovil.models.RegisterRequest;
import com.example.bombavafrontmovil.network.ApiService;
import com.example.bombavafrontmovil.network.RetrofitClient;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private boolean modoLogin = true;

    private TextView tvTitle;
    private EditText etUser, etPassword, etEmail;
    private Button btnAction;
    private TextView tvToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("BOMBA_VA", MODE_PRIVATE);
        String tokenGuardado = prefs.getString("token", null);

        if (tokenGuardado != null) {
            irAPerfil();
            return;
        }

        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_auth_action);
        tvToggle = findViewById(R.id.tv_toggle_auth);
        tvTitle = findViewById(R.id.tv_auth_title);

        tvToggle.setMovementMethod(LinkMovementMethod.getInstance());
        tvToggle.setHighlightColor(Color.TRANSPARENT);

        actualizarInterfaz();

        btnAction.setOnClickListener(v -> procesarAutenticacion());
    }

    private void alternarModo() {
        modoLogin = !modoLogin;
        etUser.setError(null);
        etPassword.setError(null);
        etEmail.setError(null);
        actualizarInterfaz();
    }

    private void actualizarInterfaz() {
        if (modoLogin) {
            tvTitle.setText("Iniciar Sesión");
            etEmail.setVisibility(View.GONE);
            btnAction.setText("ENTRAR A LA FLOTA");
            configurarTextoClickable("¿No tienes cuenta? ", "Regístrate aquí");
        } else {
            tvTitle.setText("Alistamiento");
            etEmail.setVisibility(View.VISIBLE);
            btnAction.setText("CREAR CUENTA");
            configurarTextoClickable("¿Ya eres Almirante? ", "Inicia sesión");
        }
    }

    private void configurarTextoClickable(String textoBase, String textoLink) {
        String textoCompleto = textoBase + textoLink;
        SpannableString spannable = new SpannableString(textoCompleto);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                alternarModo();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#00BCD4"));
                ds.setUnderlineText(true);
            }
        };

        spannable.setSpan(clickableSpan, textoBase.length(), textoCompleto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvToggle.setText(spannable);
    }

    private void procesarAutenticacion() {
        String user = etUser.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        boolean formularioValido = true;

        // 🔥 Validaciones Visuales
        if (user.isEmpty()) {
            etUser.setError("Usuario requerido");
            formularioValido = false;
        }
        if (pass.isEmpty()) {
            etPassword.setError("Contraseña requerida");
            formularioValido = false;
        } else if (!modoLogin && pass.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            formularioValido = false;
        }
        if (!modoLogin) {
            if (email.isEmpty()) {
                etEmail.setError("Correo requerido");
                formularioValido = false;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Correo inválido");
                formularioValido = false;
            }
        }

        if (!formularioValido) return;

        btnAction.setEnabled(false);
        btnAction.setText("ESTABLECIENDO CONEXIÓN...");

        ApiService api = RetrofitClient.getApiService();

        if (modoLogin) {
            api.login(new LoginRequest(user, pass)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    restaurarBoton();
                    if (response.isSuccessful() && response.body() != null) {
                        mostrarDialogoEstilizado("¡Conexión Exitosa!", "Bienvenido de nuevo a la flota.", true, () -> guardarTokenYProceder(response.body()));
                    } else {
                        String msg = extraerError(response);
                        mostrarDialogoEstilizado("Acceso Denegado", msg.isEmpty() ? "Usuario o contraseña incorrectos." : msg, false, null);
                    }
                }
                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    restaurarBoton();
                    mostrarDialogoEstilizado("Error de Conexión", "No pudimos contactar con el Cuartel General.", false, null);
                }
            });
        } else {
            api.register(new RegisterRequest(user, email, pass)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    restaurarBoton();
                    if (response.isSuccessful() && response.body() != null) {
                        mostrarDialogoEstilizado("¡Alistado con éxito!", "Tu cuenta ha sido creada. Prepárate para zarpar.", true, () -> guardarTokenYProceder(response.body()));
                    } else {
                        String msg = extraerError(response);
                        mostrarDialogoEstilizado("Error de Registro", msg.isEmpty() ? "Usuario o correo ya en uso." : msg, false, null);
                    }
                }
                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    restaurarBoton();
                    mostrarDialogoEstilizado("Error de Conexión", "No pudimos contactar con el servidor.", false, null);
                }
            });
        }
    }

    private String extraerError(Response<AuthResponse> response) {
        try {
            if (response.errorBody() != null) {
                JSONObject jsonError = new JSONObject(response.errorBody().string());
                if (jsonError.has("message")) return jsonError.getString("message");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "";
    }

    private void restaurarBoton() {
        btnAction.setEnabled(true);
        btnAction.setText(modoLogin ? "ENTRAR A LA FLOTA" : "CREAR CUENTA");
    }

    // Pop-Up Personalizado para el Login/Registro
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

    private void guardarTokenYProceder(AuthResponse auth) {
        String token = auth.getToken();
        String miUserId = "";

        if (auth.getUser() != null && auth.getUser().getId() != null) {
            miUserId = auth.getUser().getId();
        } else if (token != null && token.split("\\.").length == 3) {
            try {
                String payload = new String(android.util.Base64.decode(token.split("\\.")[1], android.util.Base64.URL_SAFE));
                org.json.JSONObject json = new org.json.JSONObject(payload);
                if (json.has("id")) miUserId = json.getString("id");
                else if (json.has("userId")) miUserId = json.getString("userId");
                else if (json.has("sub")) miUserId = json.getString("sub");
            } catch (Exception e) { e.printStackTrace(); }
        }

        getSharedPreferences("BOMBA_VA", MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .putString("userId", miUserId)
                .apply();

        irAPerfil();
    }

    private void irAPerfil() {
        Intent intent = new Intent(this, PantallaPerfil.class);
        startActivity(intent);
        finish();
    }
}