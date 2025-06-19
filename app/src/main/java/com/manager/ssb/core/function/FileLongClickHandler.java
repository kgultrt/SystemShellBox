// FileLongClickHandler.java
package com.manager.ssb.core.function;

import android.content.Context;
import android.view.View;

import com.manager.ssb.MainActivity;
import com.manager.ssb.dialog.FileActionDialog;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.task.NotifyingExecutorService;

public class FileLongClickHandler {
    private final Context context;
    private final NotifyingExecutorService executorService;

    public FileLongClickHandler(Context context, NotifyingExecutorService executorService) {
        this.context = context;
        this.executorService = executorService;
    }

    public void handle(FileItem item, View view) {
        new FileActionDialog(context, item, executorService, newFile -> {
            // 通知MainActivity刷新
            if (context instanceof MainActivity) {
                ((MainActivity) context).refreshAllPanels();
            }
        }).show();
    }
}
