package com.manager.ssb.dialog;

import android.content.Context;
import android.text.InputType;
import android.widget.Toast;
import android.app.Dialog;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.manager.ssb.R;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.task.TaskTypes;
import java.io.File;

public class RenameDialog {

    public interface OnRenameCallback {
        void onRenameSuccess(File newFile);
    }

    public static void show(Context context, FileItem fileItem, 
                            NotifyingExecutorService executorService, 
                            OnRenameCallback callback) {
        
        TextInputLayout inputLayout = new TextInputLayout(context);
        TextInputEditText inputEdit = new TextInputEditText(context);
        inputEdit.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        inputEdit.setText(fileItem.getName());

        inputLayout.setHint(context.getString(R.string.rename_hint));
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.addView(inputEdit);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.rename)
                .setView(inputLayout)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            MaterialButton confirm = (MaterialButton) dialog.getButton(Dialog.BUTTON_POSITIVE);
            
            confirm.setOnClickListener(v -> {
                String newName = inputEdit.getText() != null ? 
                                 inputEdit.getText().toString().trim() : "";
                if (newName.isEmpty()) {
                    inputLayout.setError(context.getString(R.string.invalid_name));
                    return;
                }

                File originalFile = fileItem.getFile();
                File newFile = new File(originalFile.getParent(), newName);
                if (newFile.exists()) {
                    inputLayout.setError(context.getString(R.string.file_name_exists));
                    return;
                }
                
                SimpleProcessingDialog progressDialog = new SimpleProcessingDialog(context);
                progressDialog.show();

                executorService.execute(() -> {
                    boolean success = originalFile.renameTo(newFile);

                    ((android.app.Activity) context).runOnUiThread(() -> {
                        progressDialog.dismiss();

                        if (success) {
                            callback.onRenameSuccess(newFile);
                            dialog.dismiss();
                            Toast.makeText(context, R.string.rename_success, Toast.LENGTH_SHORT).show();
                        } else {
                            inputLayout.setError(context.getString(R.string.rename_failed));
                        }
                    });
                }, TaskTypes.RENAME_FILE);
            });
        });
        dialog.show();
    }
}
