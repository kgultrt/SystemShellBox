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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.util.TypedValue;
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
    private final Set<String> selectedItems = new HashSet<>();
    
    // 滑动相关变量
    private float startX = 0;
    private float startY = 0;
    private boolean isSwiping = false;
    private ViewHolder swipingViewHolder = null;
    private static final float SWIPE_THRESHOLD = 20; // 滑动阈值(像素)
    private static final float LONG_PRESS_THRESHOLD = 10; // 长按移动阈值(像素)
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private boolean isLongPressTriggered = false;

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
        boolean wasMultiSelectMode = isMultiSelectMode;
        isMultiSelectMode = multiSelectMode;
        
        if (!multiSelectMode) {
            // 清除所有选中状态
            selectedItems.clear();
        }
        
        // 只有在模式真正改变时才通知更新
        if (wasMultiSelectMode != isMultiSelectMode) {
            notifyDataSetChanged();
        }
    }
    
    public void toggleSelection(FileItem item) {
        String path = item.getPath();
        
        if (selectedItems.contains(path)) {
            selectedItems.remove(path);
            if (getSelectedCount() == 0) {
                setMultiSelectMode(false);
            } else {
                notifyItemChanged(fileList.indexOf(item));
            }
        } else {
            if ("..".equals(item.getName())) return; // 屏蔽返回项
            selectedItems.add(path);
            notifyItemChanged(fileList.indexOf(item));
        }
    }
    
    public void clearSelection() {
        selectedItems.clear();
        setMultiSelectMode(false);
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
        
        // 初始化长按检测
        longPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (swipingViewHolder != null && !isSwiping && !isLongPressTriggered) {
                    isLongPressTriggered = true;
                    int position = swipingViewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        FileItem item = fileList.get(position);
                        if (longClickListener != null && !"..".equals(item.getName())) {
                            if (!isMultiSelectMode) {
                                longClickListener.onItemLongClick(item, swipingViewHolder.itemView);
                            } else {
                                // 多选模式下的长按
                                // do nothing (WIP)
                            }
                        }
                    }
                }
            }
        };
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

        // 重置视图状态
        holder.itemView.setTranslationX(0);
        holder.itemView.setAlpha(1f);
        
        // 清除可能存在的波纹效果
        if (holder.itemView.getBackground() instanceof RippleDrawable) {
            holder.itemView.getBackground().setState(new int[0]);
        }

        // 设置背景色（多选状态）
        if (isMultiSelectMode && selectedItems.contains(item.getPath())) {
            // 创建波纹效果的选择背景
            ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#88888888"));
            Drawable selectDrawable = new RippleDrawable(colorStateList, 
                    new ColorDrawable(Color.parseColor("#ADD8E6")), null);
            holder.itemView.setBackground(selectDrawable);
        } else {
            // 恢复默认选择效果（波纹）
            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            TypedArray ta = context.obtainStyledAttributes(attrs);
            Drawable defaultBackground = ta.getDrawable(0);
            ta.recycle();
            holder.itemView.setBackground(defaultBackground);
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
        holder.itemView.setTranslationX(0);
        holder.itemView.setAlpha(1f);
        
        // 清除波纹效果
        if (holder.itemView.getBackground() instanceof RippleDrawable) {
            holder.itemView.getBackground().setState(new int[0]);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
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
            
            // 设置触摸监听器来处理滑动
            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return false;
                    
                    FileItem item = fileList.get(position);
                    
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            startY = event.getY();
                            isSwiping = false;
                            isLongPressTriggered = false;
                            swipingViewHolder = ViewHolder.this;
                            
                            // 清除可能存在的波纹效果
                            if (v.getBackground() instanceof RippleDrawable) {
                                v.getBackground().setState(new int[0]);
                            }
                            
                            // 开始长按检测
                            longPressHandler.postDelayed(longPressRunnable, 500); // 500ms长按阈值
                            return false;
                            
                        case MotionEvent.ACTION_MOVE:
                            if (isMultiSelectMode) return false;
                            
                            float currentX = event.getX();
                            float currentY = event.getY();
                            float deltaX = currentX - startX;
                            float deltaY = currentY - startY;
                            
                            // 检查是否是滑动
                            if (Math.abs(deltaX) > SWIPE_THRESHOLD || Math.abs(deltaY) > SWIPE_THRESHOLD) {
                                // 取消长按检测
                                longPressHandler.removeCallbacks(longPressRunnable);
                                
                                // 检查是否是水平滑动
                                if (Math.abs(deltaX) > Math.abs(deltaY) * 1.5f) {
                                    isSwiping = true;
                                    
                                    // 移动项目视图并添加视觉反馈
                                    v.setTranslationX(deltaX);
                                    
                                    // 添加透明度效果，使滑动更明显
                                    float alpha = 1f - Math.min(0.3f, Math.abs(deltaX) / v.getWidth() * 0.5f);
                                    v.setAlpha(alpha);
                                    
                                    return true; // 消费事件
                                }
                            }
                            return false;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // 取消长按检测
                            longPressHandler.removeCallbacks(longPressRunnable);
                            
                            if (isSwiping && swipingViewHolder == ViewHolder.this) {
                                // 滑动结束，恢复位置或触发多选
                                float endX = event.getX();
                                float delta = endX - startX;
                                
                                // 如果滑动距离足够大，触发多选模式
                                if (Math.abs(delta) > v.getWidth() * 0.3f && !"..".equals(item.getName())) {
                                    if (!isMultiSelectMode) {
                                        setMultiSelectMode(true);
                                        // 直接选中当前项目
                                        selectedItems.add(item.getPath());
                                        notifyItemChanged(position);
                                    } else {
                                        toggleSelection(item);
                                    }
                                }
                                
                                // 平滑恢复位置
                                v.animate()
                                        .translationX(0)
                                        .alpha(1f)
                                        .setDuration(200)
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 确保波纹效果被清除
                                                if (v.getBackground() instanceof RippleDrawable) {
                                                    v.getBackground().setState(new int[0]);
                                                }
                                            }
                                        })
                                        .start();
                                
                                isSwiping = false;
                                swipingViewHolder = null;
                                return true;
                            }
                            
                            // 如果是长按后抬起，不触发点击事件
                            if (isLongPressTriggered) {
                                isLongPressTriggered = false;
                                // 清除波纹效果
                                if (v.getBackground() instanceof RippleDrawable) {
                                    v.getBackground().setState(new int[0]);
                                }
                                return true;
                            }
                            
                            // 清除可能存在的波纹效果
                            if (v.getBackground() instanceof RippleDrawable) {
                                v.getBackground().setState(new int[0]);
                            }
                            
                            return false;
                    }
                    return false;
                }
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