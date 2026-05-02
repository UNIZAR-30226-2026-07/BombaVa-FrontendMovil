package com.example.bombavafrontmovil;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bombavafrontmovil.models.UserShip;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import android.widget.Toast;

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

        // Botón Cañón
        if (ui.btnAtk1 != null) {
            ui.btnAtk1.setOnClickListener(v -> {
                if (idBarcoSeleccionado == null) {
                    mostrarToast("Selecciona antes un barco aliado");
                    return;
                }
                if (gestor == null || !gestor.isEsMiTurno()) {
                    mostrarToast("No es tu turno");
                    return;
                }

                tipoAtaque = 1;
                rangoAtaqueActual = 4; // Rango del cañón
                ui.ocultarInfoBarco();
                actualizarRangoAtaqueVisual();
                mostrarToast("Cañón preparado. Selecciona objetivo.");
            });
        }

        if (ui.btnAtk2 != null) {
            ui.btnAtk2.setOnClickListener(v -> {
                if (idBarcoSeleccionado != null && gestor != null) {
                    if (ui.btnAtk2.getText().toString().equalsIgnoreCase("TORPEDO")) {
                        // El torpedo no requiere apuntar a una casilla, se lanza directo
                        gestor.lanzarTorpedo(idBarcoSeleccionado);
                        board.repaintFull(gestor, idBarcoSeleccionado, posicionesRangoActual);
                    }
                }
            });
        }

        // Botón Mina
        if (ui.btnAtk3 != null) {
            ui.btnAtk3.setOnClickListener(v -> {
                if (idBarcoSeleccionado == null) {
                    mostrarToast("Selecciona antes un barco aliado");
                    return;
                }
                if (gestor == null || !gestor.isEsMiTurno()) {
                    mostrarToast("No es tu turno");
                    return;
                }

                tipoAtaque = 3;
                rangoAtaqueActual = 1; // Rango de la mina
                ui.ocultarInfoBarco();
                actualizarRangoAtaqueVisual();
                mostrarToast("Mina preparada. Selecciona dónde colocarla.");
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
            gestor.lanzarTorpedo(idBarcoSeleccionado);
            cancelarModoAtaque();
            ui.mostrar(ui.layMain);
            return;
        }

        if (tipoAtaque == 3 && idBarcoSeleccionado != null && gestor.isEsMiTurno()) {
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
        /*
        if (gestor.isInvertirPerspectiva()) {
            dir = adelante ? direccionOpuesta(b.orientation) : b.orientation;
        } else {
            dir = adelante ? b.orientation : direccionOpuesta(b.orientation);
        } */
        dir = adelante ? b.orientation : direccionOpuesta(b.orientation);
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

    private void cancelarModoAtaque() {
        tipoAtaque = 0;
        rangoAtaqueActual = 0;
        limpiarRangoAtaqueVisual();
    }

    public void actualizarCeldasBarcoMovido(String shipId, int oldX, int oldY, int newX, int newY, String orientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        posiciones.addAll(board.posicionesPara(oldX, oldY, orientation, tipo, gestor));
        posiciones.addAll(board.posicionesPara(newX, newY, orientation, tipo, gestor));

        boolean eraElSeleccionado = shipId.equals(idBarcoSeleccionado);

        if (eraElSeleccionado && tipoAtaque != 0) {
            posiciones.addAll(posicionesRangoActual);
        }

        board.repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);

        if (eraElSeleccionado && tipoAtaque != 0) {
            actualizarRangoAtaqueVisual();
        }
    }

    public void actualizarCeldasBarcoRotado(String shipId, int x, int y, String oldOrientation, String newOrientation, int tipo) {
        LinkedHashSet<Integer> posiciones = new LinkedHashSet<>();
        posiciones.addAll(board.posicionesPara(x, y, oldOrientation, tipo, gestor));
        posiciones.addAll(board.posicionesPara(x, y, newOrientation, tipo, gestor));

        boolean eraElSeleccionado = shipId.equals(idBarcoSeleccionado);

        if (eraElSeleccionado && tipoAtaque != 0) {
            posiciones.addAll(posicionesRangoActual);
        }

        board.repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);

        if (eraElSeleccionado && tipoAtaque != 0) {
            actualizarRangoAtaqueVisual();
        }
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

        board.repaintPositions(posiciones, gestor, idBarcoSeleccionado, posicionesRangoActual);
    }

    private void actualizarRangoAtaqueVisual() {
        LinkedHashSet<Integer> anteriores = new LinkedHashSet<>(posicionesRangoActual);
        posicionesRangoActual.clear();

        // Validaciones iniciales
        if (gestor == null || idBarcoSeleccionado == null || tipoAtaque == 0) {
            board.repaintPositions(anteriores, gestor, idBarcoSeleccionado, posicionesRangoActual);
            return;
        }

        BarcoLogico barco = gestor.obtenerBarcoPorId(idBarcoSeleccionado);
        if (barco == null) {
            board.repaintPositions(anteriores, gestor, idBarcoSeleccionado, posicionesRangoActual);
            return;
        }

        // LÓGICA DE CAÑÓN / MINA (Área / Manhattan)
        if (tipoAtaque == 1 || tipoAtaque == 3) { // Asumiendo 1=Cañón, 3=Mina
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
        }

        else if (tipoAtaque == 2) {
            List<int[]> celdas = barco.getCeldas();
            if (celdas == null || celdas.isEmpty()) return;

            // Encontrar la Proa del barco
            int tipX = celdas.get(0)[1];
            int tipY = celdas.get(0)[0];
            String ori = barco.orientation;

            for (int[] cell : celdas) {
                int y = cell[0];
                int x = cell[1];
                if ("N".equals(ori) && y > tipY) tipY = y;
                else if ("S".equals(ori) && y < tipY) tipY = y;
                else if ("E".equals(ori) && x > tipX) tipX = x;
                else if ("W".equals(ori) && x < tipX) tipX = x;
            }

            int filaVisualPunta = gestor.filaVisualDesdeLogica(tipY);
            posicionesRangoActual.add(filaVisualPunta * 15 + tipX);

            // Proyectar las 6 casillas hacia adelante desde esa punta
            int currentX = tipX;
            int currentY = tipY;

            for (int i = 1; i <= 6; i++) {
                if ("N".equals(ori)) currentY++;
                else if ("S".equals(ori)) currentY--;
                else if ("E".equals(ori)) currentX++;
                else if ("W".equals(ori)) currentX--;

                // Comprobamos límites del tablero
                if (currentX < 0 || currentX > 14 || currentY < 0 || currentY > 14) {
                    break;
                }

                int filaVisual = gestor.filaVisualDesdeLogica(currentY);
                posicionesRangoActual.add(filaVisual * 15 + currentX);
            }
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
        // 1. Ocultar todos por defecto
        if (ui.btnAtk1 != null) ui.btnAtk1.setVisibility(View.GONE);
        if (ui.btnAtk2 != null) ui.btnAtk2.setVisibility(View.GONE);
        if (ui.btnAtk3 != null) ui.btnAtk3.setVisibility(View.GONE);

        if (gestor == null) return;

        // 2. Obtener el barco de la partida
        BarcoLogico barco = gestor.obtenerBarcoPorId(idBarco);
        if (barco == null || barco.armas == null || barco.armas.isEmpty()) {
            android.util.Log.d("DEBUG_ARMAS", "El barco no tiene armas asignadas.");
            return;
        }

        android.util.Log.d("DEBUG_ARMAS", "Armas encontradas para este barco: " + barco.armas.toString());

        // 3. Encender los botones según lo que diga el servidor
        for (String arma : barco.armas) {
            if ("CANNON".equalsIgnoreCase(arma)) {
                if (ui.btnAtk1 != null) {
                    ui.btnAtk1.setVisibility(View.VISIBLE);
                    ui.btnAtk1.setText("CAÑÓN");
                }
            } else if ("TORPEDO".equalsIgnoreCase(arma)) {
                if (ui.btnAtk2 != null) {
                    ui.btnAtk2.setVisibility(View.VISIBLE);
                    ui.btnAtk2.setText("TORPEDO");
                }
            } else if ("MINE".equalsIgnoreCase(arma)) {
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
        if (ui != null) {
            ui.mostrarNotificacion(texto, PantallaJuegoUi.TipoNotificacion.INFO);
        }
    }

    public void mostrarError(String texto) {
        if (ui != null) {
            ui.mostrarNotificacion(texto, PantallaJuegoUi.TipoNotificacion.ERROR);
        }
    }

    public void mostrarSuccess(String texto) {
        if (ui != null) {
            ui.mostrarNotificacion(texto, PantallaJuegoUi.TipoNotificacion.SUCCESS);
        }
    }

    public void deseleccionarBarco() {
        if (idBarcoSeleccionado != null) {
            String anterior = idBarcoSeleccionado;
            idBarcoSeleccionado = null;
            cancelarModoAtaque();

            if (ui != null) {
                ui.ocultarInfoBarco();
                ui.mostrar(ui.layNoSel);
            }

            // Repintamos para quitar el tinte amarillo
            actualizarSeleccionBarco(anterior, null);
        }
    }
}