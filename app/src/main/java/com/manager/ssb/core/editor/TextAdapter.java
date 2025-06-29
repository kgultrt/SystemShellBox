package com.manager.ssb.core.editor;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;

import java.util.ArrayList;
import java.util.List;

public class TextAdapter extends RecyclerView.Adapter<TextAdapter.LineViewHolder> {

    private final List<String> lines = new ArrayList<>();
    private float textSize = 14;
    private int maxLineLength = 5000;
    private boolean showLongLineWarning = true;

    @NonNull
    @Override
    public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_text_line, parent, false);
        return new LineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LineViewHolder holder, int position) {
        String line = lines.get(position);
        
        if (line.length() > maxLineLength && showLongLineWarning) {
            // 长行处理
            String visiblePart = line.substring(0, maxLineLength);
            String warning = " [行过长，已截断]";
            
            SpannableString spannable = new SpannableString(visiblePart + warning);
            spannable.setSpan(
                new StyleSpan(Typeface.BOLD), 
                visiblePart.length(), 
                spannable.length(), 
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            holder.textView.setText(spannable);
        } else {
            holder.textView.setText(line);
        }
        
        holder.textView.setTextSize(textSize);
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }
    
    public void setLines(List<String> newLines) {
        lines.clear();
        lines.addAll(newLines);
        notifyDataSetChanged();
    }
    
    public void appendLines(List<String> newLines) {
        int start = lines.size();
        lines.addAll(newLines);
        notifyItemRangeInserted(start, newLines.size());
    }
    
    public List<String> getAllLines() {
        return new ArrayList<>(lines);
    }
    
    public void setTextSize(float size) {
        this.textSize = size;
        notifyDataSetChanged();
    }
    
    public void setShowLongLineWarning(boolean show) {
        this.showLongLineWarning = show;
    }
    
    static class LineViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        
        LineViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_line);
            // 禁用自动换行
            textView.setHorizontallyScrolling(true);
        }
    }
}