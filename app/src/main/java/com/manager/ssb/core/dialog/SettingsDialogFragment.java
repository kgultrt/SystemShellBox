package com.manager.ssb.core.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.manager.ssb.R;
import com.manager.ssb.adapter.SettingAdapter;
import com.manager.ssb.model.SettingItem;

import java.util.ArrayList;
import java.util.List;

public class SettingsDialogFragment extends AppCompatDialogFragment {

    private RecyclerView recyclerViewSettings;
    private SettingAdapter settingAdapter;
    private List<SettingItem> settingItems;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE); // 去掉默认标题栏
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_settings, container, false); // 替换为你的设置布局文件

        recyclerViewSettings = view.findViewById(R.id.recycler_view_settings);
        recyclerViewSettings.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化设置项数据
        settingItems = new ArrayList<>();
        settingItems.add(new SettingItem("通用 > 任务", "任务通知", "开启或关闭任务通知", SettingItem.TYPE_SWITCH));
        //TODO: 添加更多设置项

        settingAdapter = new SettingAdapter(getContext(), settingItems);
        recyclerViewSettings.setAdapter(settingAdapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // 设置为全屏
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);

            // 可选：设置背景透明
            // dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // 对话框消失后的处理
    }
}