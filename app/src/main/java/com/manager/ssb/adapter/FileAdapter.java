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
import android.graphics.Color;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.FileType;
import com.manager.ssb.MainActivity;
import com.manager.ssb.enums.ActivePanel;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private static final long CLICK_DEBOUNCE_INTERVAL = 300; // 300毫秒防抖间隔

    private boolean clickEnabled = true;
    private boolean longClickEnabled = true;
    
    // 多选状态相关变量
    private boolean isMultiSelectMode = false;
    private final Set<String> selectedItems = new HashSet<>(); // 使用文件路径作为唯一标识

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
    
    // 多选模式相关方法
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }
    
    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            clearSelection();
        }
        notifyDataSetChanged();
    }
    
    public void toggleSelection(FileItem item) {
        String path = item.getPath(); // 假设FileItem有getPath()方法
        if (selectedItems.contains(path)) {
            selectedItems.remove(path);
        } else {
            selectedItems.add(path);
        }
        notifyItemChanged(fileList.indexOf(item));
    }
    
    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }
    
    public Set<String> getSelectedItems() {
        return new HashSet<>(selectedItems);
    }
    
    public int getSelectedCount() {
        return selectedItems.size();
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

        // 设置背景色（多选状态）
        if (isMultiSelectMode && selectedItems.contains(item.getPath())) {
            holder.itemView.setBackgroundColor(Color.parseColor("#ADD8E6")); // 浅蓝色
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.ivIcon.setImageResource(item.isDirectory() ?
                R.drawable.ic_folder : R.drawable.ic_file);

        executorService.submit(() -> {
            // 一次性获取文件类型枚举，避免多次调用判断方法
            FileType fileType = item.resolveFileType();
    
            final int iconResId;
            switch (fileType) {
                case AUDIO:
                    iconResId = R.drawable.ic_music;
                    break;
                case TEXT:
                    iconResId = R.drawable.ic_text;
                    break;
                case COMPRESS:
                    iconResId = R.drawable.ic_zip;
                    break;
                case HTML:
                    iconResId = R.drawable.ic_web;
                    break;
                case DIRECTORY:
                    iconResId = R.drawable.ic_folder;
                    break;
                default:
                    iconResId = R.drawable.ic_file;
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
            if (isMultiSelectMode) {
                // 多选模式下的点击：切换选中状态
                toggleSelection(item);
                return;
            }
            
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
        
        // 长按监听器
        holder.itemView.setOnLongClickListener(v -> {
            if (isMultiSelectMode) {
                // 多选模式下的长按：切换选中状态
                toggleSelection(item);
                return true;
            }
            
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
        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvSize;
        TextView tvTime;
        private final GestureDetector gestureDetector;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSize = itemView.findViewById(R.id.tv_size);
            tvTime = itemView.findViewById(R.id.tv_time);
            
            // 手势检测器用于左右滑动
            gestureDetector = new GestureDetector(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 100;
                private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        
                        // 左右滑动都触发多选模式
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            // 通过itemView的parent获取RecyclerView
                            RecyclerView recyclerView = (RecyclerView) itemView.getParent();
                            if (recyclerView != null) {
                                FileAdapter adapter = (FileAdapter) recyclerView.getAdapter();
                                if (adapter != null && !adapter.isMultiSelectMode) {
                                    adapter.setMultiSelectMode(true);
                                    adapter.toggleSelection(adapter.fileList.get(position));
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            });

            itemView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false; // 返回false以便其他事件（如点击）可以继续处理
            });
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
