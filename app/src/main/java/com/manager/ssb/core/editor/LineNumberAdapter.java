package com.manager.ssb.core.editor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;

import java.util.ArrayList;
import java.util.List;

public class LineNumberAdapter extends RecyclerView.Adapter<LineNumberAdapter.LineNumberViewHolder> {

    private final List<Integer> lineNumbers = new ArrayList<>();
    private float textSize = 12;

    @NonNull
    @Override
    public LineNumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_line_number, parent, false);
        return new LineNumberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LineNumberViewHolder holder, int position) {
        holder.lineNumberView.setText(String.valueOf(lineNumbers.get(position)));
        holder.lineNumberView.setTextSize(textSize);
    }

    @Override
    public int getItemCount() {
        return lineNumbers.size();
    }
    
    public void setLineNumbers(int start, int count) {
        lineNumbers.clear();
        for (int i = start; i < start + count; i++) {
            lineNumbers.add(i);
        }
        notifyDataSetChanged();
    }
    
    public void appendLineNumbers(int count) {
        int start = lineNumbers.isEmpty() ? 1 : lineNumbers.get(lineNumbers.size() - 1) + 1;
        for (int i = 0; i < count; i++) {
            lineNumbers.add(start + i);
        }
        notifyItemRangeInserted(lineNumbers.size() - count, count);
    }
    
    public void setTextSize(float size) {
        this.textSize = size;
        notifyDataSetChanged();
    }
    
    static class LineNumberViewHolder extends RecyclerView.ViewHolder {
        TextView lineNumberView;
        
        LineNumberViewHolder(View itemView) {
            super(itemView);
            lineNumberView = itemView.findViewById(R.id.line_number);
        }
    }
}