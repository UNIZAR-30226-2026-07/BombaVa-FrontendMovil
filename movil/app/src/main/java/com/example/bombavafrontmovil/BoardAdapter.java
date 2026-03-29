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

        vh.waterView.setRotation(0);
        vh.waterView.clearColorFilter();
        vh.waterView.setImageDrawable(null);
        vh.waterView.setBackgroundResource(R.drawable.fondo_celda);
        if (vh.waterView.getBackground() != null) {
            vh.waterView.getBackground().clearColorFilter();
        }

        if (c.isTieneBarco()) {
            if (c.isEsProa()) {
                vh.waterView.setImageResource(R.drawable.barco_proa);
            } else if (c.getIndiceEnBarco() == 0 && c.getTipoBarco() > 1) {
                vh.waterView.setImageResource(R.drawable.barco_popa);
            } else {
                vh.waterView.setImageResource(R.drawable.barco_medio);
            }

            vh.waterView.setRotation(c.getDireccion() * 90);

            if (c.isEsAliado()) {
                vh.waterView.setColorFilter(Color.argb(140, 60, 120, 255), PorterDuff.Mode.SRC_ATOP);
            } else {
                vh.waterView.setColorFilter(Color.argb(140, 40, 40, 40), PorterDuff.Mode.SRC_ATOP);
            }
        }

        if (c.isSeleccionado()) {
            if (vh.waterView.getBackground() != null) {
                vh.waterView.getBackground().setColorFilter(
                        Color.argb(180, 255, 235, 59),
                        PorterDuff.Mode.SRC_ATOP
                );
            }
        }

        vh.itemView.setOnClickListener(v -> listener.onCasillaClick(c));
    }

    @Override
    public int getItemCount() {
        return tableroDatos.size();
    }
}