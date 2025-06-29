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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.model.SettingItem;

import java.util.List;

public class SettingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<SettingItem> settingItems;
    private SharedPreferences sharedPreferences;

    public SettingAdapter(Context context, List<SettingItem> settingItems) {
        this.context = context;
        this.settingItems = settingItems;
        this.sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;
        switch (viewType) {
            case SettingItem.TYPE_SWITCH:
                view = inflater.inflate(R.layout.item_setting_switch, parent, false);
                return new SwitchViewHolder(view);
            //TODO: 添加其他 ViewType 的 ViewHolder
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingItem settingItem = settingItems.get(position);
        switch (settingItem.getType()) {
            case SettingItem.TYPE_SWITCH:
                SwitchViewHolder switchViewHolder = (SwitchViewHolder) holder;
                switchViewHolder.bind(settingItem);
                break;
            //TODO: 绑定其他 ViewType 的 ViewHolder
            default:
                throw new IllegalArgumentException("Invalid view type: " + settingItem.getType());
        }
    }

    @Override
    public int getItemCount() {
        return settingItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return settingItems.get(position).getType();
    }

    // Switch ViewHolder
    public class SwitchViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView descriptionTextView;
        Switch switchView;

        public SwitchViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            switchView = itemView.findViewById(R.id.switch_view);
        }

        public void bind(SettingItem settingItem) {
            titleTextView.setText(settingItem.getTitle());
            descriptionTextView.setText(settingItem.getDescription());

            boolean isChecked = sharedPreferences.getBoolean(settingItem.getTitle(), true);
            switchView.setChecked(isChecked);

            switchView.setOnCheckedChangeListener((buttonView, isCheckedNew) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(settingItem.getTitle(), isCheckedNew);
                editor.apply();

                // TODO: 在这里处理任务通知的开启/关闭逻辑
                settingItem.setChecked(isCheckedNew);
            });
        }
    }
}