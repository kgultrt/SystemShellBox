package com.manager.ssb.dialog;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.enums.ActivePanel;

import java.io.File;

public class FileActionDialog {

    public interface OnActionCallback {
        void onSuccess(File callBack);
    }

    private final Context context;
    private final FileItem fileItem;
    private final OnActionCallback callback;
    private final NotifyingExecutorService executorService;
    private final ActivePanel activePanel;

    public FileActionDialog(@NonNull Context context, @NonNull FileItem fileItem,  
                            @NonNull NotifyingExecutorService executorService, 
                            @NonNull ActivePanel activePanel, 
                            @NonNull OnActionCallback callback) {
        this.context = context;
        this.fileItem = fileItem;
        this.executorService = executorService;
        this.callback = callback;
        this.activePanel = activePanel;
    }

    public void show() {
        new MaterialAlertDialogBuilder(context)
            .setTitle(fileItem.getName())
            .setItems(new String[]{
                    context.getString(R.string.rename),
                    context.getString(R.string.file_properties),
                    context.getString(R.string.delete)
            }, (dialog, which) -> {
                switch (which) {
                    case 0: // Rename
                        RenameDialog.show(context, fileItem, executorService, 
                                          callback::onSuccess);
                        break;
                    case 1: // Properties
                        FilePropertiesDialog.show(context, fileItem, activePanel);
                        break;
                    case 2: // Delete
                        DeleteDialog.show(context, fileItem, executorService, 
                                         callback::onSuccess);
                        break;
                }
            })
            .show();
    }
}