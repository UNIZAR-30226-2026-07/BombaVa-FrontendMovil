package com.example.bombavafrontmovil;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ConfigurarFlotaAdapter extends RecyclerView.Adapter<ConfigurarFlotaAdapter.ViewHolder> {

    private final List<CeldaVisual> celdasTablero;
    private final OnCeldaClickListener listener;

    public interface OnCeldaClickListener {
        void onCeldaClick(int posicion);
    }

    public ConfigurarFlotaAdapter(List<CeldaVisual> celdasTablero, OnCeldaClickListener listener) {
        this.celdasTablero = celdasTablero;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final FrameLayout layoutFondo;
        public final ImageView imgBarco;

        public ViewHolder(View view) {
            super(view);
            // Referencias exactas a tu item_celda_flota.xml
            layoutFondo = view.findViewById(R.id.layout_fondo);
            imgBarco = view.findViewById(R.id.img_barco);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_celda_flota, parent, false);

        // Cálculo instantáneo para evitar el parpadeo visual al cargar
        int width = parent.getMeasuredWidth();
        if (width == 0) {
            width = parent.getContext().getResources().getDisplayMetrics().widthPixels;
        }

        int size = width / 15;
        if (size > 0) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = size;
            view.setLayoutParams(layoutParams);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CeldaVisual celda = celdasTablero.get(position);

        holder.layoutFondo.setBackgroundColor(celda.colorAgua);

        if (celda.idImagenBarco != 0) {
            // Mostramos la imagen del barco y la rotamos
            holder.imgBarco.setVisibility(View.VISIBLE);
            holder.imgBarco.setImageResource(celda.idImagenBarco);
            holder.imgBarco.setRotation(celda.rotacion);

            if (celda.seleccionadaParaArma) {
                holder.imgBarco.setColorFilter(Color.argb(150, 255, 235, 59), PorterDuff.Mode.SRC_ATOP);
            } else {
                // Quitamos el filtro si no está seleccionado
                holder.imgBarco.clearColorFilter();
            }
        } else {
            // Es solo agua sin barco, ocultamos la capa de la imagen
            holder.imgBarco.setVisibility(View.GONE);
            holder.imgBarco.clearColorFilter();
            holder.imgBarco.setRotation(0f);
        }

        holder.itemView.setOnClickListener(v -> listener.onCeldaClick(position));
    }

    @Override
    public int getItemCount() {
        return celdasTablero.size();
    }
}