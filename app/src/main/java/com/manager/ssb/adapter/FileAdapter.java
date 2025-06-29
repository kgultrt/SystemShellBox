/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.MainActivity;
import com.manager.ssb.enums.ActivePanel;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final List<FileItem> fileList;
    private final OnItemClickListener listener;
    private final OnItemLongClickListener longClickListener;
    private final String panel;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private static final ThreadLocal<SimpleDateFormat> dateFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()));
    
    // 新增防抖控制
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_INTERVAL = 800; // 800毫秒防抖间隔

    private boolean clickEnabled = true;
    private boolean longClickEnabled = true;

    public void setClickEnabled(boolean enabled) {
        this.clickEnabled = enabled;
    }

    public void setLongClickEnabled(boolean enabled) {
        this.longClickEnabled = enabled;
    }
    public interface OnItemClickListener {
        void onItemClick(FileItem item);
    }
    
    // 长按功能
    public interface OnItemLongClickListener {
        void onItemLongClick(FileItem item, View view);
    }


    public FileAdapter(List<FileItem> fileList, 
                       OnItemClickListener listener,
                       OnItemLongClickListener longClickListener,
                       String panel,
                       ExecutorService executorService, 
                       Handler mainHandler) {
        this.fileList = fileList;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.panel = panel;
        this.executorService = executorService;
        this.mainHandler = mainHandler;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem item = fileList.get(position);
        Context context = holder.itemView.getContext();

        holder.ivIcon.setImageResource(item.isDirectory() ?
                R.drawable.ic_folder : R.drawable.ic_file);

        executorService.submit(() -> {
            final int iconResId;
            if (item.isAudioFile()) {
                iconResId = R.drawable.ic_music;
            } else if (item.isTextFile()) {
                iconResId = R.drawable.ic_text;
            } else {
                iconResId = item.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file;
            }

            final String sizeText = item.isDirectory() ? "" : formatSize(context, item.getSize());
            final String timeText = formatDate(item.getLastModified());

            mainHandler.post(() -> {
                holder.ivIcon.setImageResource(iconResId);
                holder.tvName.setText(item.getName());
                holder.tvSize.setText(sizeText);
                holder.tvTime.setText(timeText);
            });
        });

        // 修改后的点击监听器
        holder.itemView.setOnClickListener(v -> {
            // 禁用切换
            ((MainActivity) context).canSwichActivePanel = false;
            
            if (!clickEnabled) return;
            
            // 防抖检查
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < CLICK_DEBOUNCE_INTERVAL) {
                ((MainActivity) context).canSwichActivePanel = false;
                return;
            }
            lastClickTime = currentTime;
            
            if (listener != null) {
                listener.onItemClick(item);
            }
            
            // 启用切换
            ((MainActivity) context).canSwichActivePanel = true;
        });
        
        // 监听器
        holder.itemView.setOnLongClickListener(v -> {
            if (!longClickEnabled) return false;
            
            if ("..".equals(item.getName())) return false; // 屏蔽返回项
            
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item, v);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.ivIcon.setImageDrawable(null);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvSize;
        TextView tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSize = itemView.findViewById(R.id.tv_size);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }

    public static String formatSize(Context context, long size) {
        if (size <= 0) return "0B ";
        String[] units = context.getResources().getStringArray(R.array.size_units);
        int digitGroups = (int) (Math.log(size) / Math.log(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + units[digitGroups] + " ";
    }

    static String formatDate(long timestamp) {
        try {
            return dateFormat.get().format(new Date(timestamp));
        } catch (Exception e) {
            return "(Unknown Date)";
        }
    }
}