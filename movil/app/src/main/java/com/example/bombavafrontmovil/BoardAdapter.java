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
        public final ImageView imgMina;

        public ViewHolder(View view) {
            super(view);
            waterView = view.findViewById(R.id.view_water);
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

        vh.waterView.setRotation(0f);
        vh.waterView.clearColorFilter();
        vh.waterView.setImageDrawable(null);
        vh.waterView.setBackgroundResource(R.drawable.fondo_celda);

        if (vh.waterView.getBackground() != null) {
            vh.waterView.getBackground().clearColorFilter();
        }

        if (c.isEnRangoAtaque() && vh.waterView.getBackground() != null) {
            vh.waterView.getBackground().setColorFilter(
                    Color.argb(70, 255, 255, 180),
                    PorterDuff.Mode.SRC_ATOP
            );
        }

        if (vh.imgMina != null) {
            if (c.hasMina()) {
                android.util.Log.d("DEBUG_MINA_UI", "Intentando dibujar mina en posición: " + c.getDireccion() + " | ¿Es aliada?: " + c.isMinaAliada());
                vh.imgMina.setVisibility(View.VISIBLE);

                if (c.isMinaAliada()) {
                    vh.imgMina.setColorFilter(Color.parseColor("#4488FF"), PorterDuff.Mode.SRC_ATOP);
                } else {
                    vh.imgMina.setColorFilter(Color.parseColor("#FF4444"), PorterDuff.Mode.SRC_ATOP);
                }
            } else {
                vh.imgMina.setVisibility(View.GONE);
                vh.imgMina.clearColorFilter();
            }
        } else {
            // Si entra aquí, es que no guardaste el XML de la celda correctamente
            android.util.Log.e("DEBUG_MINA_UI", "¡ERROR! vh.imgMina es NULL. Revisa el archivo XML de la celda.");
        }

        if (c.isTieneBarco()) {
            boolean esPiezaInicial = (c.getIndiceEnBarco() == 0 && c.getTipoBarco() > 1);
            boolean esVertical = (c.getDireccion() == 0 || c.getDireccion() == 2);

            // En vertical, intercambiamos proa/popa porque tus sprites quedan al revés.
            if (esVertical) {
                if (c.isEsProa()) {
                    vh.waterView.setImageResource(R.drawable.barco_popa);
                } else if (esPiezaInicial) {
                    vh.waterView.setImageResource(R.drawable.barco_proa);
                } else {
                    vh.waterView.setImageResource(R.drawable.barco_medio);
                }
            } else {
                // En horizontal estaba bien
                if (c.isEsProa()) {
                    vh.waterView.setImageResource(R.drawable.barco_proa);
                } else if (esPiezaInicial) {
                    vh.waterView.setImageResource(R.drawable.barco_popa);
                } else {
                    vh.waterView.setImageResource(R.drawable.barco_medio);
                }
            }

            float rot;
            switch (c.getDireccion()) {
                case 0: // S
                    rot = 180f;
                    break;
                case 1: // W
                    rot = 270f;
                    break;
                case 2: // N
                    rot = 0f;
                    break;
                case 3: // E
                    rot = 90f;
                    break;
                default:
                    rot = 0f;
                    break;
            }

            vh.waterView.setRotation(rot);

            if (c.isSeleccionado()) {
                vh.waterView.setColorFilter(
                        Color.argb(110, 255, 235, 59),
                        PorterDuff.Mode.SRC_ATOP
                );
            } else if (!c.isEsAliado()) {
                vh.waterView.setColorFilter(
                        Color.argb(70, 255, 80, 80),
                        PorterDuff.Mode.SRC_ATOP
                );
            } else {
                vh.waterView.clearColorFilter();
            }
        }

        if (c.hasTorpedo()) {
            vh.waterView.setImageResource(R.drawable.ic_torpedo);

            float rotT = 0f;
            if ("N".equals(c.getDireccionTorpedo())) rotT = 0f;
            else if ("S".equals(c.getDireccionTorpedo())) rotT = 180f;
            else if ("E".equals(c.getDireccionTorpedo())) rotT = 90f;
            else if ("W".equals(c.getDireccionTorpedo())) rotT = 270f;
            vh.waterView.setRotation(rotT);

            if (!c.isTorpedoAliado()) {
                // Si es un torpedo enemigo, le ponemos un tinte rojo para asustar
                vh.waterView.setColorFilter(Color.argb(200, 255, 50, 50), PorterDuff.Mode.SRC_ATOP);
            } else {
                vh.waterView.clearColorFilter();
            }
        }

        vh.itemView.setOnClickListener(v -> listener.onCasillaClick(c));
    }

    @Override
    public int getItemCount() {
        return tableroDatos.size();
    }
}