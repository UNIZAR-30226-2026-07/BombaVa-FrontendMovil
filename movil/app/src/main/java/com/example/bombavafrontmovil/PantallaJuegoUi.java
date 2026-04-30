package com.example.bombavafrontmovil;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PantallaJuegoUi {

    public final View layNoSel;
    public final View layMain;
    public final View layMove;
    public final View layAtk;
    public final View panelInfoBarco;

    public final TextView txtInfoTitulo;
    public final TextView txtInfoGlobal;
    public final TextView txtInfoCeldas;
    public final TextView txtTurnoStatus, txtTurnoDisplay;

    public final Button btnPasarTurno;
    public final ImageButton btnPause;

    public final Button btnAtk1;
    public final Button btnAtk2;
    public final Button btnAtk3;

    public final ProgressBar barFuel;
    public final ProgressBar barAmmo;
    public final TextView txtFuel;
    public final TextView txtAmmo;

    public enum TipoNotificacion {
        INFO,
        ERROR,
        SUCCESS
    }

    private final Handler notificationHandler = new Handler(Looper.getMainLooper());
    private TextView notificationBanner;
    private Runnable hideNotificationRunnable;

    public PantallaJuegoUi(Activity activity) {
        txtTurnoDisplay = activity.findViewById(R.id.tvTurnoDisplay);
        Log.d("DEBUG_TURNO", "¿Se ha encontrado el texto del turno en el XML?: " + (txtTurnoDisplay != null));;

        layNoSel = activity.findViewById(R.id.txtNoSelection);
        layMain = activity.findViewById(R.id.layoutMainActions);
        layMove = activity.findViewById(R.id.layoutMoveActions);
        layAtk = activity.findViewById(R.id.layoutAttackActions);

        panelInfoBarco = activity.findViewById(R.id.panelInfoBarco);
        txtInfoTitulo = activity.findViewById(R.id.txtInfoTitulo);
        txtInfoGlobal = activity.findViewById(R.id.txtInfoGlobal);
        txtInfoCeldas = activity.findViewById(R.id.txtInfoCeldas);

        txtTurnoStatus = activity.findViewById(R.id.txtTurnoStatus);
        btnPasarTurno = activity.findViewById(R.id.btnPasarTurno);
        btnPause = activity.findViewById(R.id.btnPause);

        btnAtk1 = activity.findViewById(R.id.btnAtk1);
        btnAtk2 = activity.findViewById(R.id.btnAtk2);
        btnAtk3 = activity.findViewById(R.id.btnAtk3);

        barFuel = activity.findViewById(R.id.barFuel);
        barAmmo = activity.findViewById(R.id.barAmmo);
        txtFuel = activity.findViewById(R.id.txtFuel);
        txtAmmo = activity.findViewById(R.id.txtAmmo);

        if (txtInfoCeldas != null) {
            txtInfoCeldas.setText("");
            txtInfoCeldas.setVisibility(View.GONE);
        }

        mostrar(layNoSel);
        inicializarBannerNotificaciones(activity);
    }

    public void mostrar(View layout) {
        if (layNoSel != null) layNoSel.setVisibility(View.GONE);
        if (layMain != null) layMain.setVisibility(View.GONE);
        if (layMove != null) layMove.setVisibility(View.GONE);
        if (layAtk != null) layAtk.setVisibility(View.GONE);

        if (layout != null) layout.setVisibility(View.VISIBLE);
    }
    private void inicializarBannerNotificaciones(Activity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        notificationBanner = new TextView(activity);
        notificationBanner.setVisibility(View.GONE);
        notificationBanner.setAlpha(0f);
        notificationBanner.setTranslationY(-120f);
        notificationBanner.setTextColor(Color.WHITE);
        notificationBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        notificationBanner.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        notificationBanner.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP;
        params.setMargins(dp(activity, 12), dp(activity, 12), dp(activity, 12), 0);

        notificationBanner.setLayoutParams(params);
        notificationBanner.setElevation(dp(activity, 8));
        notificationBanner.setBackgroundColor(Color.parseColor("#455A64"));

        root.addView(notificationBanner);
    }

    public void mostrarNotificacion(String mensaje, TipoNotificacion tipo) {
        if (notificationBanner == null) return;

        if (hideNotificationRunnable != null) {
            notificationHandler.removeCallbacks(hideNotificationRunnable);
        }

        switch (tipo) {
            case ERROR:
                notificationBanner.setBackgroundColor(Color.parseColor("#C62828"));
                break;
            case SUCCESS:
                notificationBanner.setBackgroundColor(Color.parseColor("#2E7D32"));
                break;
            case INFO:
            default:
                notificationBanner.setBackgroundColor(Color.parseColor("#455A64"));
                break;
        }

        notificationBanner.setText(mensaje);
        notificationBanner.setVisibility(View.VISIBLE);
        notificationBanner.animate().cancel();
        notificationBanner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();

        hideNotificationRunnable = () -> ocultarNotificacion();
        notificationHandler.postDelayed(hideNotificationRunnable, 2200);
    }

    public void ocultarNotificacion() {
        if (notificationBanner == null || notificationBanner.getVisibility() != View.VISIBLE) return;

        notificationBanner.animate().cancel();
        notificationBanner.animate()
                .alpha(0f)
                .translationY(-120f)
                .setDuration(200)
                .withEndAction(() -> notificationBanner.setVisibility(View.GONE))
                .start();
    }

    private int dp(Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    public void actualizarRecursos(int fuel, int ammo) {
        if (fuel >= 0) {
            barFuel.setProgress(fuel);
            txtFuel.setText(String.valueOf(fuel));
        }
        if (ammo >= 0) {
            barAmmo.setProgress(ammo);
            txtAmmo.setText(String.valueOf(ammo));
        }
    }

    public void actualizarTurno(boolean esMiTurno, Activity activity) {
        if (esMiTurno) {
            txtTurnoStatus.setText("TU TURNO");
            txtTurnoStatus.setTextColor(activity.getResources().getColor(android.R.color.holo_green_light));
            btnPasarTurno.setEnabled(true);
            btnPasarTurno.setAlpha(1.0f);
        } else {
            txtTurnoStatus.setText("TURNO RIVAL");
            txtTurnoStatus.setTextColor(activity.getResources().getColor(android.R.color.holo_red_light));
            btnPasarTurno.setEnabled(false);
            btnPasarTurno.setAlpha(0.5f);
            mostrar(layNoSel);
        }
    }

    public void ocultarInfoBarco() {
        if (panelInfoBarco != null) panelInfoBarco.setVisibility(View.GONE);
        if (txtInfoCeldas != null) {
            txtInfoCeldas.setText("");
            txtInfoCeldas.setVisibility(View.GONE);
        }
    }

    public void mostrarInfoBarco(BarcoLogico barco) {
        if (txtInfoTitulo != null) {
            txtInfoTitulo.setText("Barco aliado");
        }

        String nombreTipo = "Barco";
        if (barco.tipo == 1) nombreTipo = "Corbeta";
        else if (barco.tipo == 3) nombreTipo = "Fragata";
        else if (barco.tipo == 5) nombreTipo = "Acorazado";

        int vision = barco.getRangoVision();

        if (txtInfoGlobal != null) {
            txtInfoGlobal.setText(
                    "Tipo: " + nombreTipo +
                            "\nVida: " + barco.hpActual + " / " + barco.hpMax +
                            "\nOrientación: " + barco.orientation +
                            "\nTamaño: " + barco.tipo + " casilla(s)" +
                            "\nVisión: " + vision
            );
        }

        if (txtInfoCeldas != null) {
            txtInfoCeldas.setText("");
            txtInfoCeldas.setVisibility(View.GONE);
        }

        if (panelInfoBarco != null) {
            panelInfoBarco.setVisibility(View.VISIBLE);
        }
    }

    // Interfaz para el callback del diálogo
    public interface PausaCallback {
        void onRespuesta(boolean aceptar);
    }

    // Metodo para bloquear/desbloquear la interacción
    public void setEstadoBloqueoRed(boolean bloqueado) {
        // Si tienes un View de "overlay" (capa transparente) en el XML, actívalo aquí
        // Si no, podemos deshabilitar los botones principales
        if (btnPasarTurno != null) {
            btnPasarTurno.setEnabled(!bloqueado);
            btnPasarTurno.setAlpha(bloqueado ? 0.5f : 1.0f);
        }
        if (btnPause != null) btnPause.setEnabled(!bloqueado);
    }

    public void mostrarDialogoPausaSolicitada(PausaCallback callback) {
        // Usamos btnPause.getContext() para obtener la actividad sin problemas
        new AlertDialog.Builder(btnPause.getContext())
                .setTitle("Pausa Solicitada")
                .setMessage("El oponente quiere pausar la partida. Si aceptas, la partida se guardará y volverás al menú.")
                .setPositiveButton("ACEPTAR", (d, w) -> callback.onRespuesta(true))
                .setNegativeButton("RECHAZAR", (d, w) -> callback.onRespuesta(false))
                .setCancelable(false)
                .show();
    }

    public void actualizarTurnoDisplay(int numeroTurno, boolean esMiTurno) {
        if (txtTurnoDisplay != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                // Ahora solo pintamos el número de turno, el estado lo pinta txtTurnoStatus
                txtTurnoDisplay.setText("Turno " + numeroTurno + " - ");
            });
        }
    }
}