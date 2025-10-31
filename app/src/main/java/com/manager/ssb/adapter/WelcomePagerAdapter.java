package com.manager.ssb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.model.WelcomePage;

import java.util.List;

public class WelcomePagerAdapter extends RecyclerView.Adapter<WelcomePagerAdapter.ViewHolder> {

    private final List<WelcomePage> pages;

    public WelcomePagerAdapter(List<WelcomePage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_welcome_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WelcomePage page = pages.get(position);
        holder.bind(page);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIllustration;
        private final TextView tvTitle;
        private final TextView tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIllustration = itemView.findViewById(R.id.ivIllustration);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }

        public void bind(WelcomePage page) {
            ivIllustration.setImageResource(page.getIllustrationRes());
            tvTitle.setText(page.getTitle());
            tvDescription.setText(page.getDescription());
        }
    }
}