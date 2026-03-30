package com.example.bombavafrontmovil;

import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.models.UserShip;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PantallaJuegoController {

    private final AppCompatActivity activity;
    private final PantallaJuegoUi ui;
    private final PantallaJuegoBoard board;

    private GestorJuego gestor;
    private Map<String, UserShip> diccionarioFlota;

    private String idBarcoSeleccionado = null;
    private int tipoAtaque = 0;
    private int rangoAtaqueActual = 0;
    private final LinkedHashSet<Integer> posicionesRangoActual = new LinkedHashSet<>();

    public PantallaJuegoController(AppCompatActivity activity, PantallaJuegoUi ui, PantallaJuegoBoard board) {
        this.activity = activity;
        this.ui = ui;
        this.board = board;
    }

    public void setGestor(GestorJuego gestor) {
        this.gestor = gestor;
    }

    public void setDiccionarioFlota(Map<String, UserShip> diccionarioFlota) {
        this.diccionarioFlota = diccionarioFlota;
    }

    public String getIdBarcoSeleccionado() {
        return idBarcoSeleccionado;
    }

    public LinkedHashSet<Integer> getPosicionesRangoActual() {
        return posicionesRangoActual;
    }

    public void configurarBotones() {
        View btnMainMove = activity.findViewById(R.id.btnMainMove);
        if (btnMainMove != null) btnMainMove.setOnClickListener(v -> ui.mostrar(ui.layMove));

        View btnMainAttack = activity.findViewById(R.id.btnMainAttack);
        if (btnMainAttack != null) {
            btnMainAttack.setOnClickListener(v -> {
                if (idBarcoSeleccionado == null) {
                    mostrarToast("Selecciona antes un barco aliado");
                    return;
                }
                actualizarBotonesArmas(idBarcoSeleccionado);
                ui.mostrar(ui.layAtk);
            });
        }

        View btnForward = activity.findViewById(R.id.btnForward);
        if (btnForward != null) btnForward.setOnClickListener(v -> moverSeleccionado(true));

        View btnBackward = activity.findViewById(R.id.btnBackward);
        if (btnBackward != null) btnBackward.setOnClickListener(v -> moverSeleccionado(false));

        View btnRotateL = activity.findViewById(R.id.btnRotateL);
        if (btnRotateL != null) btnRotateL.setOnClickListener(v -> rotarSeleccionado(-90));

        View btnRotateR = activity.findViewById(R.id.btnRotateR);
        if (btnRotateR != null) btnRotateR.setOnClickListener(v -> rotarSeleccionado(90));

        if (ui.btnAtk1 != null) ui.btnAtk1.setOnClickListener(v -> activarAtaqueCannon());
        if (ui.btnAtk2 != null) ui.btnAtk2.setOnClickListener(v -> lanzarTorpedo());
        if (ui.btnAtk3 != null) ui.btnAtk3.setOnClickListener(v -> activarAtaqueMina());

        View btnCloseInfo = activity.findViewById(R.id.btnCloseInfo);
        if (btnCloseInfo != null) btnCloseInfo.setOnClickListener(v -> ui.ocultarInfoBarco());

        View btnInfo = activity.findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                if (idBarcoSeleccionado == null || gestor == null) return;
                BarcoLogico barco = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
                if (barco != null && barco.esAliado) ui.mostrarInfoBarco(barco);
            });
        }
    }

    public void manejarToqueCasilla(Casilla c) {
        if (gestor == null) return;

        BarcoLogico barco = gestor.obtenerBarcoEn(c.getFila(), c.getColumna());

        if (barco != null && barco.esAliado) {
            android.util.Log.d(
                    "DEBUG_SELECT",
                    "Seleccionado shipId=" + barco.id +
                            " x=" + barco.x +
                            " y=" + barco.y +
                            " orientation=" + barco.orientation +
                            " tipo=" + barco.tipo
            );

            String anterior = idBarcoSeleccionado;
            idBarcoSeleccionado = barco.id;

            cancelarModoAtaque();
            actualizarBotonesArmas(barco.id);
            ui.ocultarInfoBarco();
            ui.mostrar(ui.layMain);
            actualizarSeleccionBarco(anterior, idBarcoSeleccionado);
            return;
        }

        if (tipoAtaque == 1 && idBarcoSeleccionado != null && gestor.isEsMiTurno()) {
            if (!c.isEnRangoAtaque()) {
                mostrarToast("Esa casilla está fuera de rango");
                return;
            }

            android.util.Log.d(
                    "DEBUG_CLICK",
                    "Ataque click filaVisual=" + c.getFila() +
                            " col=" + c.getColumna() +
                            " -> yLogica=" + gestor.filaLogicaDesdeVisual(c.getFila())
            );

            gestor.dispararCannon(idBarcoSeleccionado, c.getFila(), c.getColumna());
            cancelarModoAtaque();
            ui.mostrar(ui.layMain);
            return;
        }

        if (tipoAtaque == 2 && idBarcoSeleccionado != null && gestor.isEsMiTurno()) {
            if (!c.isEnRangoAtaque()) {
                mostrarToast("Esa casilla está fuera de rango");
                return;
            }
            gestor.colocarMina(idBarcoSeleccionado, c.getFila(), c.getColumna());
            cancelarModoAtaque();
            ui.mostrar(ui.layMain);
            return;
        }

        String anterior = idBarcoSeleccionado;
        idBarcoSeleccionado = null;
        cancelarModoAtaque();
        ui.ocultarInfoBarco();
        ui.mostrar(ui.layNoSel);
        actualizarSeleccionBarco(anterior, null);
    }

    private void moverSeleccionado(boolean adelante) {
        if (idBarcoSeleccionado == null || gestor == null) return;
        if (!gestor.isEsMiTurno()) {
            mostrarToast("No es tu turno");
            return;
        }

        BarcoLogico b = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
        if (b == null) return;

        cancelarModoAtaque();

        String dir;
        if (gestor.isInvertirPerspectiva()) {
            dir = adelante ? direccionOpuesta(b.orientation) : b.orientation;
        } else {
            dir = adelante ? b.orientation : direccionOpuesta(b.orientation);
        }

        gestor.moverBarco(idBarcoSeleccionado, dir);
        ui.mostrar(ui.layMain);
    }

    private void rotarSeleccionado(int grados) {
        if (idBarcoSeleccionado == null || gestor == null) return;
        if (!gestor.isEsMiTurno()) {
            mostrarToast("No es tu turno");
            return;
        }
        cancelarModoAtaque();
        gestor.rotarBarco(idBarcoSeleccionado, grados);
        ui.mostrar(ui.layMain);
    }

    private void activarAtaqueCannon() {
        if (idBarcoSeleccionado == null) {
            mostrarToast("Selecciona antes un barco aliado");
            return;
        }
        if (gestor == null || !gestor.isEsMiTurno()) {
            mostrarToast("No es tu turno");
            return;
        }

        tipoAtaque = 1;
        rangoAtaqueActual = 4;
        ui.ocultarInfoBarco();
        actualizarRangoAtaqueVisual();
        mostrarToast("Selecciona la casilla objetivo para disparar");
    }

    private void activarAtaqueMina() {
        if (idBarcoSeleccionado == null) {
            mostrarToast("Selecciona antes un barco aliado");
            return;
        }
        if (gestor == null || !gestor.isEsMiTurno()) {
            mostrarToast("No es tu turno");
            return;
        }

        tipoAtaque = 2;
        rangoAtaqueActual = 1;
        ui.ocultarInfoBarco();
        actualizarRangoAtaqueVisual();
        mostrarToast("Selecciona la casilla donde quieres colocar la mina");
    }

    private void lanzarTorpedo() {
        if (gestor == null || idBarcoSeleccionado == null) return;
        if (!gestor.isEsMiTurno()) {
            mostrarToast("No es tu turno");
            return;
        }

        cancelarModoAtaque();
        gestor.lanzarTorpedo(idBarcoSeleccionado);
        ui.mostrar(ui.layMain);
    }

    private void cancelarModoAtaque() {
        tipoAtaque = 0;
        rangoAtaqueActual = 0;
        limpiarRangoAtaqueVisual();
    }

    public void actualizarCeldasBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        posiciones.addAll(board.posicionesPara(oldX, oldY, orientation, tipo, gestor));
        posiciones.addAll(board.posicionesPara(newX, newY, orientation, tipo, gestor));
        posiciones.addAll(posicionesRangoActual);

        board.repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);
        if (shipId.equals(idBarcoSeleccionado) && tipoAtaque != 0) actualizarRangoAtaqueVisual();
    }

    public void actualizarCeldasBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        posiciones.addAll(board.posicionesPara(x, y, oldOrientation, tipo, gestor));
        posiciones.addAll(board.posicionesPara(x, y, newOrientation, tipo, gestor));
        posiciones.addAll(posicionesRangoActual);

        board.repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);
        if (shipId.equals(idBarcoSeleccionado) && tipoAtaque != 0) actualizarRangoAtaqueVisual();
    }

    private void actualizarSeleccionBarco(String idAnterior, String idNuevo) {
        if (gestor == null) return;

        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        for (BarcoLogico b : gestor.getFlota()) {
            boolean afecta = (idAnterior != null && idAnterior.equals(b.id)) ||
                    (idNuevo != null && idNuevo.equals(b.id));
            if (!afecta) continue;
            posiciones.addAll(board.posicionesPara(b.x, b.y, b.orientation, b.tipo, gestor));
        }

        for (Integer pos : posiciones) {
            Casilla casilla = board.getMatriz().get(pos);
            String shipId = casilla.getIdBarcoStr();
            casilla.setSeleccionado(shipId != null && shipId.equals(idNuevo) && casilla.isEsAliado());
        }
        board.repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);
    }

    private void actualizarRangoAtaqueVisual() {
        LinkedHashSet<Integer> anteriores = new LinkedHashSet<>(posicionesRangoActual);
        posicionesRangoActual.clear();

        if (gestor == null || idBarcoSeleccionado == null || rangoAtaqueActual <= 0) {
            board.repaintPositions(anteriores, gestor, idBarcoSeleccionado, posicionesRangoActual);
            return;
        }

        BarcoLogico barco = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
        if (barco == null) {
            board.repaintPositions(anteriores, gestor, idBarcoSeleccionado, posicionesRangoActual);
            return;
        }

        List<int[]> origenes = barco.getCeldas();

        for (Casilla c : board.getMatriz()) {
            int x = c.getColumna();
            int yLogica = gestor.filaLogicaDesdeVisual(c.getFila());

            boolean enRango = false;
            for (int[] origen : origenes) {
                int dx = Math.abs(x - origen[1]);
                int dy = Math.abs(yLogica - origen[0]);

                if ((dx + dy) <= rangoAtaqueActual) {
                    enRango = true;
                    break;
                }
            }
            if (enRango) posicionesRangoActual.add(c.getFila() * 15 + c.getColumna());
        }

        LinkedHashSet<Integer> aRepintar = new LinkedHashSet<>(anteriores);
        aRepintar.addAll(posicionesRangoActual);
        board.repaintPositions(aRepintar, gestor, idBarcoSeleccionado, posicionesRangoActual);
    }

    private void limpiarRangoAtaqueVisual() {
        if (posicionesRangoActual.isEmpty()) return;
        LinkedHashSet<Integer> anteriores = new LinkedHashSet<>(posicionesRangoActual);
        posicionesRangoActual.clear();
        board.repaintPositions(anteriores, gestor, idBarcoSeleccionado, posicionesRangoActual);
    }

    private void actualizarBotonesArmas(String idBarco) {
        if (ui.btnAtk1 != null) ui.btnAtk1.setVisibility(View.GONE);
        if (ui.btnAtk2 != null) ui.btnAtk2.setVisibility(View.GONE);
        if (ui.btnAtk3 != null) ui.btnAtk3.setVisibility(View.GONE);

        if (diccionarioFlota == null) return;

        UserShip ship = diccionarioFlota.get(idBarco);
        android.util.Log.d("DEBUG_ARMAS", "UserShip asociado: " + (ship != null ? ship.getId() : "null"));

        if (ship == null || ship.getWeaponTemplates() == null) return;

        for (UserShip.WeaponItem w : ship.getWeaponTemplates()) {
            if ("cannon-base".equals(w.slug)) {
                if (ui.btnAtk1 != null) {
                    ui.btnAtk1.setVisibility(View.VISIBLE);
                    ui.btnAtk1.setText("CAÑÓN");
                }
            } else if ("torpedo-v1".equals(w.slug)) {
                if (ui.btnAtk2 != null) {
                    ui.btnAtk2.setVisibility(View.VISIBLE);
                    ui.btnAtk2.setText("TORPEDO");
                }
            } else if ("mine-v1".equals(w.slug)) {
                if (ui.btnAtk3 != null) {
                    ui.btnAtk3.setVisibility(View.VISIBLE);
                    ui.btnAtk3.setText("MINA");
                }
            }
        }
    }

    private String direccionOpuesta(String orientation) {
        if ("N".equals(orientation)) return "S";
        if ("S".equals(orientation)) return "N";
        if ("E".equals(orientation)) return "W";
        if ("W".equals(orientation)) return "E";
        return orientation;
    }

    public void mostrarToast(String texto) {
        Toast.makeText(activity, texto, Toast.LENGTH_SHORT).show();
    }
}