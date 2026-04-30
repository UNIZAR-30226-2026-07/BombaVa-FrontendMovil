package com.example.bombavafrontmovil;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bombavafrontmovil.models.RankingUser;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<RankingUser> rankingList;

    public RankingAdapter(List<RankingUser> rankingList) {
        this.rankingList = rankingList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvPos, tvName, tvElo;
        public final View layoutBase;

        public ViewHolder(View view) {
            super(view);
            tvPos = view.findViewById(R.id.tvRankPos);
            tvName = view.findViewById(R.id.tvRankName);
            tvElo = view.findViewById(R.id.tvRankElo);
            layoutBase = view.findViewById(R.id.layItemRanking);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ranking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RankingUser user = rankingList.get(position);
        int rank = position + 1;

        holder.tvPos.setText("#" + rank);
        holder.tvName.setText(user.getUsername());
        holder.tvElo.setText(user.getEloRating() + " ⚔️");

        // Estética Clash Royale para el Top 3
        if (rank == 1) {
            holder.tvPos.setTextColor(Color.parseColor("#FFD700")); // Oro
            holder.tvName.setTextColor(Color.parseColor("#FFD700"));
        } else if (rank == 2) {
            holder.tvPos.setTextColor(Color.parseColor("#C0C0C0")); // Plata
            holder.tvName.setTextColor(Color.parseColor("#C0C0C0"));
        } else if (rank == 3) {
            holder.tvPos.setTextColor(Color.parseColor("#CD7F32")); // Bronce
            holder.tvName.setTextColor(Color.parseColor("#CD7F32"));
        } else {
            holder.tvPos.setTextColor(Color.WHITE);
            holder.tvName.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return rankingList.size();
    }
}