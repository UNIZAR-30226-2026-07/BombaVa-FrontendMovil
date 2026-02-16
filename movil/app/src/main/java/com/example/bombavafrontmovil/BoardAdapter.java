package com.example.bombavafrontmovil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.ViewHolder> {
    private List<Casilla> tableroDatos;
    private OnCasillaClickListener listener;

    public interface OnCasillaClickListener { void onCasillaClick(Casilla casilla); }

    public BoardAdapter(List<Casilla> dataSet, OnCasillaClickListener listener) {
        this.tableroDatos = dataSet;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View waterView;
        public ViewHolder(View view) { super(view); waterView = view.findViewById(R.id.view_water); }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup vg, int vt) {
        View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.celda, vg, false);
        vg.post(() -> {
            int size = vg.getWidth() / 15;
            v.getLayoutParams().width = size;
            v.getLayoutParams().height = size;
            v.requestLayout();
        });
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int pos) {
        Casilla c = tableroDatos.get(pos);

        // Color por tipo y selección
        if (c.isSeleccionado()) vh.waterView.setBackgroundResource(R.color.selection_yellow);
        else if (c.getTipoBarco() == 1) vh.waterView.setBackgroundResource(R.color.ship_1);
        else if (c.getTipoBarco() == 3) vh.waterView.setBackgroundResource(R.color.ship_3);
        else if (c.getTipoBarco() == 5) vh.waterView.setBackgroundResource(R.color.ship_5);
        else vh.waterView.setBackgroundResource(R.color.sea_blue);

        vh.itemView.setOnClickListener(v -> {
            int targetId = c.getIdBarco();
            // Selección grupal por ID
            for (Casilla casilla : tableroDatos) {
                if (targetId != -1 && casilla.getIdBarco() == targetId) {
                    casilla.setSeleccionado(true);
                } else {
                    casilla.setSeleccionado(false);
                }
            }
            notifyDataSetChanged();
            listener.onCasillaClick(c);
        });
    }

    @Override
    public int getItemCount() { return tableroDatos.size(); }
}