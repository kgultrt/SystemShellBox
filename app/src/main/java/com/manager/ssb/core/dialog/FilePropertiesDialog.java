package com.manager.ssb.dialog;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.manager.ssb.model.FileItem;
import com.manager.ssb.MainActivity;
import com.manager.ssb.R;
import com.manager.ssb.Application;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilePropertiesDialog {

    public static void show(Context context, FileItem item) {
        File file = item.getFile();
        StringBuilder sb = new StringBuilder();
        
        boolean isLeftPanel = ((MainActivity) context).getActivePanel();
        File panelRoot = ((MainActivity) context).getCurrentDir();

        sb.append(Application.getAppContext().getString(R.string.file_properties_name)).append(item.getName()).append("\n");
        sb.append(Application.getAppContext().getString(R.string.file_properties_path)).append(file.getAbsolutePath()).append("\n");
        sb.append(Application.getAppContext().getString(R.string.file_properties_size)).append(Formatter.formatFileSize(context, item.getSize())).append("\n");

        sb.append(Application.getAppContext().getString(R.string.file_properties_mod_time)).append(formatTime(item.getLastModified())).append("\n");

        sb.append(Application.getAppContext().getString(R.string.file_properties_is_dir)).append(file.isDirectory() ? Application.getAppContext().getString(R.string.file_properties_yes) : Application.getAppContext().getString(R.string.file_properties_no)).append("\n");
        sb.append(Application.getAppContext().getString(R.string.file_properties_read)).append(file.canRead()).append("\n").append(Application.getAppContext().getString(R.string.file_properties_write)).append(file.canWrite()).append("\n");

        sb.append("\n\n");
        sb.append(Application.getAppContext().getString(R.string.file_properties_panel)).append(isLeftPanel ? Application.getAppContext().getString(R.string.file_properties_panel_left) : Application.getAppContext().getString(R.string.file_properties_panel_right)).append("\n");
        sb.append(Application.getAppContext().getString(R.string.file_properties_panel_another)).append(isLeftPanel ? ((MainActivity) context).getRightDir().getAbsolutePath() : ((MainActivity) context).getLeftDir().getAbsolutePath());

        new MaterialAlertDialogBuilder(context)
                .setTitle(Application.getAppContext().getString(R.string.file_properties_title))
                .setMessage(sb.toString())
                .setPositiveButton(Application.getAppContext().getString(R.string.confirm), null)
                .show();
    }

    private static String formatTime(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));
    }
}