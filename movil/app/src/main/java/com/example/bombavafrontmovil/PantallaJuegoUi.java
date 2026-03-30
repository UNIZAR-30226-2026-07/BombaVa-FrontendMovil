package com.example.bombavafrontmovil;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
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
    public final TextView txtTurnoStatus;

    public final Button btnPasarTurno;
    public final ImageButton btnPause;

    public final Button btnAtk1;
    public final Button btnAtk2;
    public final Button btnAtk3;

    public final ProgressBar barFuel;
    public final ProgressBar barAmmo;
    public final TextView txtFuel;
    public final TextView txtAmmo;

    public PantallaJuegoUi(Activity activity) {
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
    }

    public void mostrar(View layout) {
        if (layNoSel != null) layNoSel.setVisibility(View.GONE);
        if (layMain != null) layMain.setVisibility(View.GONE);
        if (layMove != null) layMove.setVisibility(View.GONE);
        if (layAtk != null) layAtk.setVisibility(View.GONE);

        if (layout != null) layout.setVisibility(View.VISIBLE);
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

        int vision = 0;
        if (barco.tipo == 1) vision = 4;
        else if (barco.tipo == 3) vision = 3;
        else if (barco.tipo == 5) vision = 2;

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
}