package com.example.bombavafrontmovil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ConfigurarFlotaAdapter extends RecyclerView.Adapter<ConfigurarFlotaAdapter.ViewHolder> {

    private final List<Integer> coloresCeldas;
    private final OnCeldaClickListener listener;

    public interface OnCeldaClickListener {
        void onCeldaClick(int posicion);
    }

    public ConfigurarFlotaAdapter(List<Integer> coloresCeldas, OnCeldaClickListener listener) {
        this.coloresCeldas = coloresCeldas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_celda_flota, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View vistaAgua = holder.itemView.findViewById(R.id.vista_color_celda);
        vistaAgua.setBackgroundColor(coloresCeldas.get(position));

        holder.itemView.setOnClickListener(v -> listener.onCeldaClick(position));
    }

    @Override
    public int getItemCount() {
        return coloresCeldas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}