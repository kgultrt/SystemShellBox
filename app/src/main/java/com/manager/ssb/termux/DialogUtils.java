package com.manager.ssb.termux;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Selection;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public final class DialogUtils {

    public interface TextSetListener {
        void onTextSet(String text);
    }

    public static void textInput(Activity activity, int titleText, String initialText,
                                 int positiveButtonText, final TextSetListener onPositive,
                                 int neutralButtonText, final TextSetListener onNeutral,
                                 int negativeButtonText, final TextSetListener onNegative,
                                 final DialogInterface.OnDismissListener onDismiss) {
        
        // 创建 TextInputLayout 和 TextInputEditText (MD3 风格)
        TextInputLayout textInputLayout = new TextInputLayout(activity);
        textInputLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        final TextInputEditText input = new TextInputEditText(textInputLayout.getContext());
        input.setSingleLine();
        
        // 设置 hint 为标题
        if (titleText != 0) {
            textInputLayout.setHint(activity.getResources().getString(titleText));
        }
        
        if (initialText != null) {
            input.setText(initialText);
            Selection.setSelection(input.getText(), initialText.length());
        }

        textInputLayout.addView(input);

        // 使用数组来持有对话框引用 - 使用 androidx.appcompat.app.AlertDialog
        final androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];

        input.setImeActionLabel(activity.getResources().getString(positiveButtonText), KeyEvent.KEYCODE_ENTER);
        input.setOnEditorActionListener((v, actionId, event) -> {
            onPositive.onTextSet(input.getText().toString());
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
            return true;
        });

        // MD3 对话框有内置的 padding，不需要手动设置
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        // 设置 Material Design 3 推荐的间距
        float dipInPixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1, activity.getResources().getDisplayMetrics());
        int paddingHorizontal = Math.round(24 * dipInPixels);
        int paddingTop = Math.round(20 * dipInPixels);
        
        layout.setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0);
        layout.addView(textInputLayout);

        // 使用 MaterialAlertDialogBuilder 替代 AlertDialog.Builder
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
            .setView(layout)
            .setPositiveButton(positiveButtonText, (d, whichButton) -> 
                onPositive.onTextSet(input.getText().toString()));

        // 如果有标题，设置标题（MD3 中标题是可选的）
        if (titleText != 0) {
            builder.setTitle(titleText);
        }

        if (onNeutral != null) {
            builder.setNeutralButton(neutralButtonText, (dialog, which) -> 
                onNeutral.onTextSet(input.getText().toString()));
        }

        if (onNegative == null) {
            builder.setNegativeButton(android.R.string.cancel, null);
        } else {
            builder.setNegativeButton(negativeButtonText, (dialog, which) -> 
                onNegative.onTextSet(input.getText().toString()));
        }

        if (onDismiss != null) {
            builder.setOnDismissListener(onDismiss);
        }

        // 创建对话框并保存引用
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}

