package com.manager.ssb.core.editor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TextEditor";
    private TextEditor textEditor;
    private String filePath;
    private String fileName;
    private TextView tvTitle;
    private boolean hasEdited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texteditor);
        
        // 获取UI元素
        tvTitle = findViewById(R.id.tvTitle);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnSave = findViewById(R.id.btnSave);
        ImageButton btnUndo = findViewById(R.id.btnUndo);
        ImageButton btnRedo = findViewById(R.id.btnRedo);
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        
        textEditor = findViewById(R.id.codeEditor);

        // 获取传递的文件信息
        Intent intent = getIntent();
        filePath = intent.getStringExtra("file_path");
        fileName = intent.getStringExtra("file_name");

        // 设置标题
        if (fileName != null) {
            tvTitle.setText(fileName);
        } else {
            tvTitle.setText("untitled");
        }

        // 设置按钮点击事件
        btnBack.setOnClickListener(v -> onBackPressed());
        btnSave.setOnClickListener(v -> saveFile());
        btnUndo.setOnClickListener(v -> {
            textEditor.undo();
            // 更新编辑状态
            hasEdited = true;
            updateTitle();
        });
        btnRedo.setOnClickListener(v -> {
            textEditor.redo();
            // 更新编辑状态
            hasEdited = true;
            updateTitle();
        });
        btnSettings.setOnClickListener(v -> showSettings());

        // 加载文件内容
        if (filePath != null) {
            loadFileContent(filePath);
        } else {
            // 没有文件路径时的默认文本
            textEditor.setText("text");
        }
        
        // 监听文本变化（实现编辑状态检测）
        textEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不需要实现
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!hasEdited) {
                    hasEdited = true;
                    updateTitle();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 不需要实现
            }
        });
    }

    private void loadFileContent(String filePath) {
        final File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "File not found: " + filePath, Toast.LENGTH_SHORT).show();
            return;
        }

        // 在新线程中读取文件
        new Thread(() -> {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
                
                // 在主线程更新UI
                runOnUiThread(() -> {
                    textEditor.setText(content.toString());
                    hasEdited = false;
                    updateTitle();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void saveFile() {
        if (filePath == null) {
            Toast.makeText(this, "Save as functionality not implemented", Toast.LENGTH_SHORT).show();
            return;
        }

        final File file = new File(filePath);
        final String content = textEditor.getText().toString();
        
        new Thread(() -> {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                
                runOnUiThread(() -> {
                    hasEdited = false;
                    updateTitle();
                    Toast.makeText(MainActivity.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    private void updateTitle() {
        if (fileName != null) {
            tvTitle.setText(hasEdited ? "* " + fileName : fileName);
        }
    }
    
    private void showSettings() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Editor Settings")
            .setItems(new String[]{"Theme", "Font Size", "Key Bindings"}, (dialog, which) -> {
                switch (which) {
                    case 0:
                        Toast.makeText(MainActivity.this, "Theme selection", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(MainActivity.this, "Font size adjustment", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(MainActivity.this, "Key bindings settings", Toast.LENGTH_SHORT).show();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (hasEdited) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Save changes?")
                .setMessage("Your changes will be lost if you don't save them")
                .setPositiveButton("Save", (d, w) -> {
                    saveFile();
                    finish();
                })
                .setNegativeButton("Discard", (d, w) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }
}