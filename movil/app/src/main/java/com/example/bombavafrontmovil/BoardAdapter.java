package com.example.bombavafrontmovil;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.ViewHolder> {

    private final List<Casilla> tableroDatos;
    private final OnCasillaClickListener listener;

    public interface OnCasillaClickListener {
        void onCasillaClick(Casilla casilla);
    }

    public BoardAdapter(List<Casilla> dataSet, OnCasillaClickListener listener) {
        this.tableroDatos = dataSet;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView waterView;
        public final ImageView imgBarco;
        public final ImageView imgTorpedo;
        public final ImageView imgMina;

        public ViewHolder(View view) {
            super(view);
            waterView = view.findViewById(R.id.view_water);
            imgBarco = view.findViewById(R.id.imgBarco);
            imgTorpedo = view.findViewById(R.id.imgTorpedo);
            imgMina = view.findViewById(R.id.imgMina);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup vg, int vt) {
        View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.celda, vg, false);
        vg.post(() -> {
            int size = vg.getWidth() / 15;
            if (size > 0) {
                v.getLayoutParams().width = size;
                v.getLayoutParams().height = size;
                v.requestLayout();
            }
        });
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int pos) {
        Casilla c = tableroDatos.get(pos);

        // =========================
        // RESET GENERAL
        // =========================
        vh.waterView.setBackgroundResource(R.drawable.fondo_celda);
        if (vh.waterView.getBackground() != null) {
            vh.waterView.getBackground().clearColorFilter();
        }

        vh.imgBarco.setVisibility(View.GONE);
        vh.imgBarco.setImageDrawable(null);
        vh.imgBarco.clearColorFilter();
        vh.imgBarco.setRotation(0f);

        vh.imgTorpedo.setVisibility(View.GONE);
        vh.imgTorpedo.setImageDrawable(null);
        vh.imgTorpedo.clearColorFilter();
        vh.imgTorpedo.setRotation(0f);

        vh.imgMina.setVisibility(View.GONE);
        vh.imgMina.setImageDrawable(null);
        vh.imgMina.clearColorFilter();
        vh.imgMina.setRotation(0f);

        // =========================
        // FONDO: SOLO AGUA / NIEBLA / RANGO
        // =========================
        if (!c.isVisible()) {
            if (vh.waterView.getBackground() != null) {
                vh.waterView.getBackground().setColorFilter(
                        Color.argb(210, 10, 20, 35),
                        PorterDuff.Mode.SRC_ATOP
                );
            }
        } else if (c.isEnRangoAtaque()) {
            if (vh.waterView.getBackground() != null) {
                vh.waterView.getBackground().setColorFilter(
                        Color.argb(70, 255, 255, 180),
                        PorterDuff.Mode.SRC_ATOP
                );
            }
        }

        // =========================
        // BARCOS: SOLO PNG DEL BARCO
        // =========================
        if (c.isTieneBarco() && c.isVisible() && c.getVidaActual() > 0) {
            vh.imgBarco.setVisibility(View.VISIBLE);

            boolean barcoRoto = c.getVidaMax() > 0 && c.getVidaActual() <= (c.getVidaMax() / 2);
            boolean esExtremoInicial = (c.getIndiceEnBarco() == 0 && c.getTipoBarco() > 1);
            boolean esExtremoFinal = c.isEsProa();
            boolean esVertical = (c.getDireccion() == 0 || c.getDireccion() == 2);

            int resId;

            if (c.getTipoBarco() <= 1) {
                resId = barcoRoto ? R.drawable.barco_medio_roto : R.drawable.barco_medio;
            } else if (esVertical) {
                if (esExtremoFinal) {
                    resId = barcoRoto ? R.drawable.barco_popa_roto : R.drawable.barco_popa;
                } else if (esExtremoInicial) {
                    resId = barcoRoto ? R.drawable.barco_proa_roto : R.drawable.barco_proa;
                } else {
                    resId = barcoRoto ? R.drawable.barco_medio_roto : R.drawable.barco_medio;
                }
            } else {
                if (esExtremoFinal) {
                    resId = barcoRoto ? R.drawable.barco_proa_roto : R.drawable.barco_proa;
                } else if (esExtremoInicial) {
                    resId = barcoRoto ? R.drawable.barco_popa_roto : R.drawable.barco_popa;
                } else {
                    resId = barcoRoto ? R.drawable.barco_medio_roto : R.drawable.barco_medio;
                }
            }

            vh.imgBarco.setImageResource(resId);

            float rot;
            switch (c.getDireccion()) {
                case 0:
                    rot = 180f; // S
                    break;
                case 1:
                    rot = 270f; // W
                    break;
                case 2:
                    rot = 0f;   // N
                    break;
                case 3:
                    rot = 90f;  // E
                    break;
                default:
                    rot = 0f;
                    break;
            }
            vh.imgBarco.setRotation(rot);

            // Amarillo suave SOLO al PNG del barco seleccionado
            if (c.isSeleccionado()) {
                vh.imgBarco.setColorFilter(
                        Color.argb(110, 255, 235, 59),
                        PorterDuff.Mode.SRC_ATOP
                );
            }
            // Rojo suave SOLO al PNG del barco enemigo
            else if (!c.isEsAliado()) {
                vh.imgBarco.setColorFilter(
                        Color.argb(160, 255, 0, 0),
                        PorterDuff.Mode.SRC_ATOP
                );
            } else {
                vh.imgBarco.clearColorFilter();
            }
        }

        // =========================
        // MINAS
        // =========================
        if (c.hasMina() && c.isVisible()) {
            vh.imgMina.setVisibility(View.VISIBLE);
            vh.imgMina.setImageResource(R.drawable.ic_mina);

            if (c.isMinaAliada()) {
                vh.imgMina.setColorFilter(
                        Color.argb(255, 100, 255, 100),
                        PorterDuff.Mode.MULTIPLY
                );
            } else {
                vh.imgMina.clearColorFilter();
            }
        }

        // =========================
        // TORPEDOS
        // =========================
        if (c.hasTorpedo() && c.isVisible()) {
            vh.imgTorpedo.setVisibility(View.VISIBLE);
            vh.imgTorpedo.setImageResource(R.drawable.ic_torpedo);
            vh.imgTorpedo.setRotation(c.getRotacionTorpedo());

            if (!c.isTorpedoAliado()) {
                vh.imgTorpedo.setColorFilter(
                        Color.argb(200, 255, 50, 50),
                        PorterDuff.Mode.SRC_ATOP
                );
            } else {
                vh.imgTorpedo.clearColorFilter();
            }
        }

        vh.itemView.setOnClickListener(v -> listener.onCasillaClick(c));
    }

    @Override
    public int getItemCount() {
        return tableroDatos.size();
    }
}