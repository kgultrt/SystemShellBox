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

// FileLongClickHandler.java
package com.manager.ssb.core.function;

import android.content.Context;
import android.view.View;

import com.manager.ssb.MainActivity;
import com.manager.ssb.dialog.FileActionDialog;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.enums.ActivePanel;

import java.io.File;

public class FileLongClickHandler {
    private final Context context;
    private final NotifyingExecutorService executorService;
    private final ActivePanel activePanel;

    public FileLongClickHandler(Context context, NotifyingExecutorService executorService, ActivePanel activePanel) {
        this.context = context;
        this.executorService = executorService;
        this.activePanel = activePanel;
    }

    public void handle(FileItem item, View view, ActivePanel activePanel) {
        if (!(context instanceof MainActivity)) return;
        
        MainActivity activity = (MainActivity) context;
        
        // 禁用切换
        activity.canSwichActivePanel = false;
        
        // 禁用非活动面板
        if (activePanel == ActivePanel.LEFT) {
            activity.adapterRight.setClickEnabled(false);
            activity.adapterRight.setLongClickEnabled(false);
        } else {
            activity.adapterLeft.setClickEnabled(false);
            activity.adapterLeft.setLongClickEnabled(false);
        }
        
        // 取消之前的恢复任务
        activity.disableHandler.removeCallbacks(activity.enableClicksRunnable);
        // 350ms后恢复所有面板
        activity.disableHandler.postDelayed(activity.enableClicksRunnable, 300);
        
        // 创建回调处理
        FileActionDialog.OnActionCallback callback = new FileActionDialog.OnActionCallback() {
            @Override
            public void onRenameSuccess(File newFile) {
                activity.refreshAllPanels();
            }

            @Override
            public void onDeleteSuccess(File deletedFile) {
                activity.refreshAllPanels();
            }
        };
        
        new FileActionDialog(context, item, executorService, activePanel, callback).show();
    }
}