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

        // Comprobamos si ya hay sesión al arrancar
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

        // Activamos que el TextView pueda recibir clics en partes específicas
        tvToggle.setMovementMethod(LinkMovementMethod.getInstance());
        tvToggle.setHighlightColor(Color.TRANSPARENT); // Quita el fondo gris feo al hacer clic

        // Configuramos la interfaz inicial (Login)
        actualizarInterfaz();

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                procesarAutenticacion();
            }
        });
    }

    private void alternarModo() {
        modoLogin = !modoLogin;
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
                // Aquí cambiamos el color del texto clicable
                ds.setColor(Color.parseColor("#00BCD4"));
                ds.setUnderlineText(true); // Lo subrayamos
            }
        };

        spannable.setSpan(clickableSpan, textoBase.length(), textoCompleto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvToggle.setText(spannable);
    }

    private void procesarAutenticacion() {
        String user = etUser.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            mostrarDialogoError("Faltan datos", "Por favor, rellena todos los campos requeridos.");
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("ESTABLECIENDO CONEXIÓN...");

        ApiService api = RetrofitClient.getApiService();

        if (modoLogin) {
            api.login(new LoginRequest(user, pass)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    restaurarBoton();
                    if (response.isSuccessful() && response.body() != null) {
                        mostrarDialogoExito("¡Conexión Exitosa!", "Bienvenido de nuevo a la flota.", response.body());
                    } else {
                        mostrarDialogoError("Acceso Denegado", "Usuario o contraseña incorrectos.");
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    restaurarBoton();
                    mostrarDialogoError("Error de Conexión", "No pudimos contactar con el Cuartel General.");
                }
            });
        } else {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                restaurarBoton();
                mostrarDialogoError("Faltan datos", "El correo es obligatorio para alistarse.");
                return;
            }

            api.register(new RegisterRequest(user, email, pass)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    restaurarBoton();
                    if (response.isSuccessful() && response.body() != null) {
                        mostrarDialogoExito("¡Alistado con éxito!", "Tu cuenta ha sido creada. Prepárate para zarpar.", response.body());
                    } else {
                        mostrarDialogoError("Error de Registro", "Ese usuario o correo electrónico ya están en uso.");
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    restaurarBoton();
                    mostrarDialogoError("Error de Conexión", "No pudimos contactar con el servidor.");
                }
            });
        }
    }

    private void restaurarBoton() {
        btnAction.setEnabled(true);
        btnAction.setText(modoLogin ? "ENTRAR A LA FLOTA" : "CREAR CUENTA");
    }

    private void mostrarDialogoError(String titulo, String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    private void mostrarDialogoExito(String titulo, String mensaje, AuthResponse authData) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Avanzar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        guardarTokenYProceder(authData);
                    }
                })
                .setIcon(R.drawable.ic_check_green)
                .setCancelable(false)
                .show();
    }

    private void guardarTokenYProceder(AuthResponse auth) {
        String token = auth.getToken();
        String miUserId = "";

        // Intentamos sacarlo del objeto User (por si algún día el servidor sí lo envía)
        if (auth.getUser() != null && auth.getUser().getId() != null) {
            miUserId = auth.getUser().getId();
        }
        // Extraemos el ID decodificando el propio Token JWT (Infalible)
        else if (token != null && token.split("\\.").length == 3) {
            try {
                // El token tiene 3 partes. La segunda [1] es el payload que tiene nuestros datos.
                String payload = new String(android.util.Base64.decode(token.split("\\.")[1], android.util.Base64.URL_SAFE));
                org.json.JSONObject json = new org.json.JSONObject(payload);

                // Buscamos las palabras más comunes que usan los servidores para el ID
                if (json.has("id")) miUserId = json.getString("id");
                else if (json.has("userId")) miUserId = json.getString("userId");
                else if (json.has("sub")) miUserId = json.getString("sub");

                android.util.Log.d("DEBUG_BOMBA", "ID extraído del token: " + miUserId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Lo guardamos todo a salvo en las preferencias
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