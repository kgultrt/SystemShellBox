package com.manager.ssb.core.editor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.manager.ssb.R;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class TextEditorActivity extends Activity {

    private EditText editText;
    private TextView fileNameView;
    private String filePath;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        filePath = getIntent().getStringExtra("file_path");
        fileName = getIntent().getStringExtra("file_name");

        fileNameView = findViewById(R.id.tv_file_name);
        fileNameView.setText(fileName != null ? fileName : "Untitled");

        editText = findViewById(R.id.editText);

        // 保存按钮
        ImageButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveText());

        if (filePath != null) {
            loadText(filePath);
        } else {
            Toast.makeText(this, getString(R.string.cannot_open), Toast.LENGTH_SHORT).show();
        }
    }

    /* ------------------ 文件读取（自动编码探测） ------------------ */
    private void loadText(String path) {
        new Thread(() -> {
            String content = tryRead(path, "UTF-8");
            if (content == null) content = tryRead(path, "GBK");

            final String finalText = content != null ?
                    content : getString(R.string.cannot_reading) + " (未知编码)";
            new Handler(Looper.getMainLooper()).post(() -> editText.setText(finalText));
        }).start();
    }

    private String tryRead(String path, String charset) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), charset))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return null; // 试错
        }
    }

    /* ------------------ 保存功能 ------------------ */
    private void saveText() {
        if (filePath == null) return;

        new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(editText.getText().toString().getBytes("UTF-8"));
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.save_success), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.save_failed) + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}