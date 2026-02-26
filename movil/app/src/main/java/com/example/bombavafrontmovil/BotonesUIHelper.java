package com.example.bombavafrontmovil;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.Button;

public class BotonesUIHelper {

    private final Button btnShip5, btnShip3, btnShip1;
    private final Button btnAm, btnMi, btnTo;

    public BotonesUIHelper(Button btnShip5, Button btnShip3, Button btnShip1,
                           Button btnAm, Button btnMi, Button btnTo) {
        this.btnShip5 = btnShip5; this.btnShip3 = btnShip3; this.btnShip1 = btnShip1;
        this.btnAm = btnAm; this.btnMi = btnMi; this.btnTo = btnTo;
    }

    public void resaltarBarco(int tamano) {
        resetearFiltros(btnShip5, btnShip3, btnShip1);
        if (tamano == 5) aplicarResalte(btnShip5, btnShip3, btnShip1);
        else if (tamano == 3) aplicarResalte(btnShip3, btnShip5, btnShip1);
        else if (tamano == 1) aplicarResalte(btnShip1, btnShip5, btnShip3);
    }

    public void resaltarArma(String armaTemporal) {
        resetearFiltros(btnAm, btnMi, btnTo);
        if (armaTemporal.equals("Ametralladora")) aplicarResalte(btnAm, btnMi, btnTo);
        else if (armaTemporal.equals("Misil")) aplicarResalte(btnMi, btnAm, btnTo);
        else if (armaTemporal.equals("Torpedo")) aplicarResalte(btnTo, btnAm, btnMi);
    }

    public void actualizarBotonesArmas(int tamanoBarco, boolean tieneAm, boolean tieneMi, boolean tieneTo) {
        // La Ametralladora está en todos (si no se ha equipado ya)
        btnAm.setVisibility(tieneAm ? View.GONE : View.VISIBLE);

        // El Misil solo en barcos de tamaño 3 o 5
        if (tamanoBarco >= 3) {
            btnMi.setVisibility(tieneMi ? View.GONE : View.VISIBLE);
        } else {
            btnMi.setVisibility(View.GONE);
        }

        // El Torpedo SOLO en el barco gigante (tamaño 5)
        if (tamanoBarco == 5) {
            btnTo.setVisibility(tieneTo ? View.GONE : View.VISIBLE);
        } else {
            btnTo.setVisibility(View.GONE);
        }
    }

    public void ocultarBarcosColocados(boolean b5, boolean b3, boolean b1) {
        btnShip5.setVisibility(b5 ? View.GONE : View.VISIBLE);
        btnShip3.setVisibility(b3 ? View.GONE : View.VISIBLE);
        btnShip1.setVisibility(b1 ? View.GONE : View.VISIBLE);
    }

    private void resetearFiltros(Button b1, Button b2, Button b3) {
        b1.setAlpha(1.0f); b2.setAlpha(1.0f); b3.setAlpha(1.0f);
        b1.getBackground().mutate().clearColorFilter();
        b2.getBackground().mutate().clearColorFilter();
        b3.getBackground().mutate().clearColorFilter();
    }

    private void aplicarResalte(Button activo, Button inactivo1, Button inactivo2) {
        activo.getBackground().mutate().setColorFilter(Color.argb(120, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
        inactivo1.setAlpha(0.6f);
        inactivo2.setAlpha(0.6f);
    }
}