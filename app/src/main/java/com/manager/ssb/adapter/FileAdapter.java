/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.FileType;
import com.manager.ssb.MainActivity;
import com.manager.ssb.enums.Sence;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    // 整理 + 稍微好一点的状态管理！
    private final List<FileItem> fileList;
    private final OnItemClickListener listener;
    private final OnItemLongClickListener longClickListener;
    private final String panel;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private boolean clickEnabled = true;
    private boolean longClickEnabled = true;

    private boolean isMultiSelectMode = false;
    private final Set<String> selectedItems = new HashSet<>();

    private float startX = 0;
    private float startY = 0;
    private boolean isSwiping = false;
    private ViewHolder swipingViewHolder = null;

    private static final float SWIPE_THRESHOLD = 20f;

    private final Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;
    private boolean isLongPressTriggered = false;

    private Sence sence;

    private String highlightedItemPath = null;
    private boolean shouldScrollToHighlighted = false;

    private Drawable defaultBackground;

    private static final ThreadLocal<SimpleDateFormat> dateFormat =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                }
            };

    public interface OnItemClickListener {
        void onItemClick(FileItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FileItem item, View view);
    }

    public FileAdapter(
            List<FileItem> fileList,
            OnItemClickListener listener,
            OnItemLongClickListener longClickListener,
            String panel,
            Sence sence,
            ExecutorService executorService,
            Handler mainHandler
    ) {
        this.fileList = fileList;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.panel = panel;
        this.sence = sence;
        this.executorService = executorService;
        this.mainHandler = mainHandler;

        longPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (swipingViewHolder != null && !isSwiping && !isLongPressTriggered) {
                    isLongPressTriggered = true;
                    int position = swipingViewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        FileItem item = fileList.get(position);
                        if (longClickListener != null && !"..".equals(item.getName())) {
                            longClickListener.onItemLongClick(item, swipingViewHolder.itemView);
                        }
                    }
                }
            }
        };
    }

    /* ===================== 对外接口 ===================== */

    public void setClickEnabled(boolean enabled) {
        this.clickEnabled = enabled;
    }

    public void setLongClickEnabled(boolean enabled) {
        this.longClickEnabled = enabled;
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void setMultiSelectMode(boolean enable) {
        isMultiSelectMode = enable;
        if (!enable) selectedItems.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(FileItem item) {
        String path = item.getPath();
        if (selectedItems.contains(path)) {
            selectedItems.remove(path);
            if (selectedItems.isEmpty()) {
                setMultiSelectMode(false);
            }
        } else {
            if (!"..".equals(item.getName())) {
                selectedItems.add(path);
            }
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
    }

    public Set<String> getSelectedItems() {
        return new HashSet<>(selectedItems);
    }

    public Sence getSence() {
        return sence;
    }

    public void setSence(Sence sence) {
        this.sence = sence;
        notifyDataSetChanged();
    }

    public void highlightItem(String itemPath) {
        highlightedItemPath = itemPath;
        shouldScrollToHighlighted = true;
        notifyDataSetChanged();
    }

    public void clearHighlight() {
        highlightedItemPath = null;
        notifyDataSetChanged();
    }

    /* ===================== RecyclerView ===================== */

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);

        TypedArray ta = parent.getContext()
                .obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        Drawable bg = ta.getDrawable(0);
        ta.recycle();
        defaultBackground = bg != null ? bg.mutate() : null;

        return new ViewHolder(view);
    }

    private void resetViewState(ViewHolder holder) {
        View v = holder.itemView;
        v.animate().cancel();
        v.setTranslationX(0f);
        v.setAlpha(1f);
        v.setPressed(false);
        v.setActivated(false);
        v.setSelected(false);

        if (defaultBackground != null) {
            v.setBackground(defaultBackground.getConstantState().newDrawable().mutate());
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        resetViewState(holder);

        FileItem item = fileList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(item.getName());
        holder.tvSize.setText(item.isDirectory() ? "" : formatSize(context, item.getSize()));
        holder.tvTime.setText(formatDate(item.getLastModified()));
        holder.ivIcon.setImageResource(item.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file);

        if (highlightedItemPath != null && highlightedItemPath.equals(item.getPath())) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E9"));
            if (shouldScrollToHighlighted) {
                shouldScrollToHighlighted = false;
                holder.itemView.post(() -> {
                    RecyclerView rv = (RecyclerView) holder.itemView.getParent();
                    if (rv != null) rv.smoothScrollToPosition(holder.getAdapterPosition());
                });
            }
        } else if (isMultiSelectMode && selectedItems.contains(item.getPath())) {
            holder.itemView.setBackground(new ColorDrawable(Color.parseColor("#ADD8E6")));
        }

        executorService.submit(() -> {
            FileType type = item.resolveFileType();
            int icon;
            switch (type) {
                case AUDIO: icon = R.drawable.ic_music; break;
                case TEXT: icon = R.drawable.ic_text; break;
                case COMPRESS: icon = R.drawable.ic_zip; break;
                case HTML: icon = R.drawable.ic_web; break;
                case APK: icon = R.drawable.ic_android; break;
                case DIRECTORY: icon = R.drawable.ic_folder; break;
                default: icon = R.drawable.ic_file;
            }
            mainHandler.post(() -> {
                int p = holder.getAdapterPosition();
                if (p != RecyclerView.NO_POSITION &&
                        fileList.get(p).getPath().equals(item.getPath())) {
                    holder.ivIcon.setImageResource(icon);
                }
            });
        });

        holder.itemView.setOnClickListener(v -> {
            if (!clickEnabled) return;
            if (isMultiSelectMode) {
                toggleSelection(item);
                return;
            }
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    /* ===================== ViewHolder ===================== */

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIcon;
        TextView tvName, tvSize, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSize = itemView.findViewById(R.id.tv_size);
            tvTime = itemView.findViewById(R.id.tv_time);

            itemView.setOnTouchListener((v, e) -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                FileItem item = fileList.get(pos);

                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!longClickEnabled) return false;
                        swipingViewHolder = this;
                        startX = e.getX();
                        startY = e.getY();
                        isSwiping = false;
                        isLongPressTriggered = false;
                        longPressHandler.postDelayed(longPressRunnable, 500);
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getX() - startX;
                        float dy = e.getY() - startY;
                        if (Math.abs(dx) > SWIPE_THRESHOLD &&
                                Math.abs(dx) > Math.abs(dy)) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                            isSwiping = true;
                            v.setTranslationX(dx);
                            v.setAlpha(1f - Math.min(0.3f,
                                    Math.abs(dx) / v.getWidth()));
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        if (isSwiping) {
                            float delta = e.getX() - startX;
                            if (Math.abs(delta) > v.getWidth() * 0.3f &&
                                    !"..".equals(item.getName())) {
                                setMultiSelectMode(true);
                                selectedItems.add(item.getPath());
                            }
                            v.animate().translationX(0).alpha(1f).setDuration(200).start();
                        }
                        isSwiping = false;
                        swipingViewHolder = null;
                        isLongPressTriggered = false;
                        return false;
                }
                return false;
            });
        }
    }

    /* ===================== Utils ===================== */

    public static String formatSize(Context context, long size) {
        if (size <= 0) return "0B ";
        String[] units = context.getResources().getStringArray(R.array.size_units);
        int group = (int) (Math.log(size) / Math.log(1024));
        if (group >= units.length) group = units.length - 1;
        return new DecimalFormat("#,##0.#")
                .format(size / Math.pow(1024, group)) + units[group] + " ";
    }

    static String formatDate(long time) {
        try {
            return dateFormat.get().format(new Date(time));
        } catch (Exception e) {
            return "(Unknown)";
        }
    }
}
