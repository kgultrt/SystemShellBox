package com.manager.ssb.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;

public class FileConflictDialog {

    public interface ConflictResolution {
        int OVERWRITE = 0;
        int SKIP = 1;
        int KEEP_BOTH = 2;
    }

    public interface ConflictHandler {
        void onConflictResolved(int action, boolean applyToAll);
    }

    public static void show(@NonNull Context context, 
                            @NonNull String filename,
                            @NonNull ConflictHandler handler) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_file_conflict, null);
        RadioGroup resolutionGroup = view.findViewById(R.id.resolutionGroup);
        CheckBox applyToAllCheckbox = view.findViewById(R.id.applyToAllCheckbox);

        // 设置默认选项为"移动并替换"
        resolutionGroup.check(R.id.overwriteRadio);

        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.file_exists)
            .setMessage(filename)
            .setView(view)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                int selectedId = resolutionGroup.getCheckedRadioButtonId();
                int action = ConflictResolution.OVERWRITE;
                
                if (selectedId == R.id.skipRadio) {
                    action = ConflictResolution.SKIP;
                } else if (selectedId == R.id.keepBothRadio) {
                    action = ConflictResolution.KEEP_BOTH;
                }
                
                handler.onConflictResolved(action, applyToAllCheckbox.isChecked());
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}