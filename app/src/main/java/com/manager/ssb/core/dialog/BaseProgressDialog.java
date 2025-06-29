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

// BaseProgressDialog.java
package com.manager.ssb.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;

public class BaseProgressDialog {

    protected final Context context;
    protected Dialog dialog;

    public BaseProgressDialog(@NonNull Context context) {
        this.context = context;
        createDialog();
    }

    protected void createDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public Dialog getDialog() {
        return dialog;
    }
}