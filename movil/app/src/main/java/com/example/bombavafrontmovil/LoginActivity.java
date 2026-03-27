package com.example.bombavafrontmovil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private boolean modoLogin = true; // true = Iniciar Sesión, false = Registro

    private TextView tvTitle, tvToggle;
    private EditText etUser, etPassword;
    private Button btnAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvTitle = findViewById(R.id.tv_auth_title);
        tvToggle = findViewById(R.id.tv_toggle_auth);
        etUser = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_auth_action);

        // Cambiar entre Login y Registro
        tvToggle.setOnClickListener(v -> alternarModo());

        // Botón principal de acción
        btnAction.setOnClickListener(v -> procesarAutenticacion());
    }

    private void alternarModo() {
        modoLogin = !modoLogin;
        if (modoLogin) {
            tvTitle.setText("Iniciar Sesión");
            btnAction.setText("ENTRAR A LA FLOTA");
            tvToggle.setText("¿No tienes cuenta? Regístrate aquí");
        } else {
            tvTitle.setText("Alistamiento");
            btnAction.setText("CREAR CUENTA");
            tvToggle.setText("¿Ya eres Almirante? Inicia sesión");
        }
        // Limpiamos los campos al cambiar de modo
        etUser.setText("");
        etPassword.setText("");
    }

    private void procesarAutenticacion() {
        String user = etUser.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "¡Faltan credenciales, Almirante!", Toast.LENGTH_SHORT).show();
            return;
        }

        // SIMULACIÓN: Aquí iría tu conexión a la base de datos/API
        if (modoLogin) {
            Toast.makeText(this, "Bienvenido de vuelta, " + user, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Cuenta creada con éxito para " + user, Toast.LENGTH_SHORT).show();
        }

        // Navegamos a la pantalla de perfil (simulado)
        irAPerfil();
    }

    private void irAPerfil() {
        // Asumiendo que crearás una PerfilActivity. 
        // Si aún no la tienes, esto fallará hasta que la crees.
        Intent intent = new Intent(this, PantallaPerfil.class);
        startActivity(intent);
        finish(); // Cerramos el login para que no pueda volver atrás con el botón del móvil
    }
}