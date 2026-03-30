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

        public ViewHolder(View view) {
            super(view);
            waterView = view.findViewById(R.id.view_water);
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

        if (c.isTieneBarco()) {
            boolean esPiezaInicial = (c.getIndiceEnBarco() == 0 && c.getTipoBarco() > 1);
            boolean esExtremo = c.isEsProa() || esPiezaInicial;
            boolean esVertical = (c.getDireccion() == 0 || c.getDireccion() == 2);

            // Mantener SIEMPRE el sprite correcto: proa es proa, popa es popa
            if (c.isEsProa()) {
                vh.waterView.setImageResource(R.drawable.barco_proa);
            } else if (esPiezaInicial) {
                vh.waterView.setImageResource(R.drawable.barco_popa);
            } else {
                vh.waterView.setImageResource(R.drawable.barco_medio);
            }

            float rot = (c.getDireccion() * 90f);

            // Solo en vertical, los extremos necesitan media vuelta extra
            if (esExtremo && esVertical) {
                rot += 180f;
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

        vh.itemView.setOnClickListener(v -> listener.onCasillaClick(c));
    }

    @Override
    public int getItemCount() {
        return tableroDatos.size();
    }
}