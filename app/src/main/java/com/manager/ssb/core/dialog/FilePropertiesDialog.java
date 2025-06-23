package com.manager.ssb.dialog;

import android.content.Context;
import android.text.format.Formatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.MainActivity;
import com.manager.ssb.Application;
import com.manager.ssb.enums.ActivePanel;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilePropertiesDialog {

    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static void show(Context context, FileItem item, ActivePanel activePanel) {
        if (!(context instanceof MainActivity)) return;
        
        MainActivity activity = (MainActivity) context;
        File file = item.getFile();
        
        // 使用资源ID代替硬编码的字符串拼接
        String[] properties = buildProperties(activity, item, file);
        
        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.file_properties_title)
            .setMessage(formatMessage(activity, properties, activePanel))
            .setPositiveButton(R.string.confirm, null)
            .show();
    }

    private static String[] buildProperties(Context context, FileItem item, File file) {
        return new String[] {
            context.getString(R.string.file_properties_name, item.getName()),
            context.getString(R.string.file_properties_path, file.getAbsolutePath()),
            context.getString(R.string.file_properties_size, Formatter.formatFileSize(context, item.getSize())),
            context.getString(R.string.file_properties_mod_time, formatTime(item.getLastModified())),
            context.getString(R.string.file_properties_is_dir, 
                file.isDirectory() ? 
                    context.getString(R.string.file_properties_yes) : 
                    context.getString(R.string.file_properties_no)),
            context.getString(R.string.file_properties_read, file.canRead()),
            context.getString(R.string.file_properties_write, file.canWrite())
        };
    }

    private static CharSequence formatMessage(MainActivity activity, String[] properties, ActivePanel activePanel) {
        StringBuilder sb = new StringBuilder();
        
        // 添加基本属性
        for (String property : properties) {
            sb.append(property).append("\n");
        }
        
        // 添加面板信息
        boolean isLeftPanel;
        if (activePanel == ActivePanel.LEFT) {
            isLeftPanel = true;
        } else {
            isLeftPanel = false;
        }
        
        sb.append(activity.getString(R.string.file_properties_panel,
            isLeftPanel ? 
                activity.getString(R.string.file_properties_panel_left) : 
                activity.getString(R.string.file_properties_panel_right)));
        
        sb.append("\n\n");
        
        sb.append(activity.getString(R.string.file_properties_panel_another,
            isLeftPanel ? 
                activity.getRightDir().getAbsolutePath() : 
                activity.getLeftDir().getAbsolutePath()));
        
        return sb;
    }

    private static String formatTime(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
}