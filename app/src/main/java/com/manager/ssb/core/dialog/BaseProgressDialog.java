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