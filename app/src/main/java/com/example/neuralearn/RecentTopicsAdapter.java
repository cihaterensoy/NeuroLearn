package com.example.neuralearn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecentTopicsAdapter extends RecyclerView.Adapter<RecentTopicsAdapter.ViewHolder> {

    private List<QuizSession> sessions = new ArrayList<>();
    private OnQuizItemClickListener listener;

    public interface OnQuizItemClickListener {
        void onQuizClick(QuizSession session);
        void onQuizLongClick(QuizSession session);
    }

    public RecentTopicsAdapter(OnQuizItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<QuizSession> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_topic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizSession session = sessions.get(position);
        holder.bind(session);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuizClick(session);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onQuizLongClick(session);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRecentTitle, tvRecentDesc, tvRecentScore;
        private ImageView ivRecentIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvRecentTitle = itemView.findViewById(R.id.tvRecentTitle);
            tvRecentDesc = itemView.findViewById(R.id.tvRecentDesc);
            tvRecentScore = itemView.findViewById(R.id.tvRecentScore);
            ivRecentIcon = itemView.findViewById(R.id.ivRecentIcon);
        }

        void bind(QuizSession session) {
            // Dosya adını kısalt
            String filename = session.getFilename();
            if (filename.length() > 20) {
                filename = filename.substring(0, 17) + "...";
            }

            tvRecentTitle.setText(filename);
            tvRecentDesc.setText(session.getCreatedAt());
            tvRecentScore.setText(String.format("%.0f%%", session.getSuccessRate()));

            // Başarı oranına göre renk ayarla
            int color;
            if (session.getSuccessRate() >= 70) {
                color = android.R.color.holo_green_dark;
            } else if (session.getSuccessRate() >= 50) {
                color = android.R.color.holo_orange_dark;
            } else {
                color = android.R.color.holo_red_dark;
            }
            tvRecentScore.setTextColor(itemView.getContext().getColor(color));
        }
    }
}