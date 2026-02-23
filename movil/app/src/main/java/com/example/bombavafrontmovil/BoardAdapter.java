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

    public interface OnCasillaClickListener { void onCasillaClick(Casilla casilla); }

    public BoardAdapter(List<Casilla> dataSet, OnCasillaClickListener listener) {
        this.tableroDatos = dataSet;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView waterView;
        public ViewHolder(View view) {
            super(view);
            waterView = (ImageView) view.findViewById(R.id.view_water);
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

        // 1. LIMPIEZA
        vh.waterView.setRotation(0);
        vh.waterView.clearColorFilter();
        vh.waterView.setImageDrawable(null);
        vh.waterView.setBackgroundResource(R.drawable.fondo_celda);
        vh.waterView.getBackground().clearColorFilter();

        // 2. ¿HAY BARCO?
        if (c.isTieneBarco()) {

            // A) IMAGEN
            if (c.isEsProa()) vh.waterView.setImageResource(R.drawable.barco_proa);
            else if (c.getIndiceEnBarco() == 0 && c.getTipoBarco() > 1) vh.waterView.setImageResource(R.drawable.barco_popa);
            else vh.waterView.setImageResource(R.drawable.barco_medio);

            // B) ROTACIÓN
            vh.waterView.setRotation(c.getDireccion() * 90);

            // C) TINTES E ILUMINACIÓN
            if (c.isSeleccionado()) {
                // --- SOLUCIÓN VISUAL ---
                // Usamos Color.argb(alfa, rojo, verde, azul).
                // 100 de alfa significa que es semi-transparente.
                // SRC_ATOP pinta el color SOLO donde hay barco, respetando la transparencia.
                vh.waterView.setColorFilter(Color.argb(100, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            }
            else if (c.getVidaCelda() <= 0) {
                vh.waterView.setColorFilter(Color.DKGRAY, PorterDuff.Mode.MULTIPLY);
            }
            else if (c.getVidaCelda() == 1) {
                vh.waterView.setColorFilter(Color.parseColor("#E64A19"), PorterDuff.Mode.SRC_ATOP);
            }
            else if (!c.isEsAliado()) {
                vh.waterView.setColorFilter(Color.parseColor("#FFCDD2"), PorterDuff.Mode.MULTIPLY);
            }

        } else {
            // AGUA
            if (c.isSeleccionado()) {
                // Si es agua seleccionada, teñimos el fondo suavemente
                vh.waterView.getBackground().setColorFilter(Color.parseColor("#8081D4FA"), PorterDuff.Mode.SRC_ATOP);
            }
        }

        vh.itemView.setOnClickListener(v -> listener.onCasillaClick(c));
    }

    @Override
    public int getItemCount() { return tableroDatos.size(); }
}