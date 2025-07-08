package com.manager.ssb.core.editor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.core.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TextEditor";
    private TextEditor textEditor;
    private String filePath;
    private String fileName;
    private TextView tvTitle;
    private boolean hasEdited = false;

    // 配置键名
    private static final String KEY_THEME = "editor.theme";
    private static final String KEY_FONT_SIZE = "editor.font_size";
    private static final String KEY_KEY_BINDINGS = "editor.key_bindings";
    
    // 默认值
    private static final String DEFAULT_THEME = "light";
    private static final int DEFAULT_FONT_SIZE = 14; // sp
    private static final String DEFAULT_KEY_BINDINGS = "default";

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
        
        // 应用保存的设置
        applySavedSettings();
    }

    private void applySavedSettings() {
        // 应用主题设置
        applyTheme();
        
        // 应用字体大小设置
        applyFontSize();
        
        // 应用按键绑定设置
        applyKeyBindings();
    }
    
    private void applyTheme() {
        String theme = Config.get(KEY_THEME, DEFAULT_THEME);
        if ("dark".equals(theme)) {
            setDarkTheme();
        } else {
            setLightTheme();
        }
    }
    
    private void setLightTheme() {
        textEditor.setTextColor(Color.BLACK);
        textEditor.setBackgroundColor(Color.WHITE);
        textEditor.setCommentColor(Color.GRAY);
        textEditor.setKeywordColor(Color.BLUE);
        textEditor.setBaseWordColor(Color.DKGRAY);
        textEditor.setStringColor(Color.RED);
        textEditor.setTextHighlightColor(Color.argb(255, 0, 120, 215));
    }
    
    private void setDarkTheme() {
        textEditor.setTextColor(Color.WHITE);
        textEditor.setBackgroundColor(Color.BLACK);
        textEditor.setCommentColor(Color.LTGRAY);
        textEditor.setKeywordColor(Color.CYAN);
        textEditor.setBaseWordColor(Color.LTGRAY);
        textEditor.setStringColor(Color.MAGENTA);
        textEditor.setTextHighlightColor(Color.argb(255, 100, 180, 255));
    }
    
    private void applyFontSize() {
        int fontSize = Config.get(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        textEditor.setTextSize(fontSize);
    }
    
    private void applyKeyBindings() {
        String keyBindings = Config.get(KEY_KEY_BINDINGS, DEFAULT_KEY_BINDINGS);
        // 这里根据不同的按键绑定方案设置编辑器
        // 实际实现需要TextEditor支持不同的按键绑定方案
        if ("vim".equals(keyBindings)) {
            // 设置Vim风格的按键绑定
        } else if ("emacs".equals(keyBindings)) {
            // 设置Emacs风格的按键绑定
        } else {
            // 设置默认按键绑定
        }
    }

    private void loadFileContent(String filePath) {
        final File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "File not found: " + filePath, Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建进度对话框
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Loading File");
        builder.setMessage("Please wait...");
    
        // 创建水平进度条（不确定进度）
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(50, 50, 50, 50);
        builder.setView(progressBar);
    
        // 添加取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
    
        AlertDialog progressDialog = builder.create();
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 在新线程中读取文件
        new Thread(() -> {
            StringBuilder content = new StringBuilder();
            final boolean[] isCancelled = {false};
        
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 检查是否取消
                    if (progressDialog == null || !progressDialog.isShowing()) {
                        isCancelled[0] = true;
                        break;
                    }
                    content.append(line).append('\n');
                }
            
                // 在主线程更新UI
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                
                    if (!isCancelled[0]) {
                        textEditor.setText(content.toString());
                        hasEdited = false;
                        updateTitle();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
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
                        showThemeSelection();
                        break;
                    case 1:
                        showFontSizeSelection();
                        break;
                    case 2:
                        showKeyBindingsSelection();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showThemeSelection() {
        List<String> themes = Arrays.asList("Light", "Dark");
        String currentTheme = Config.get(KEY_THEME, DEFAULT_THEME);
        int selectedIndex = "dark".equals(currentTheme) ? 1 : 0;
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes.toArray(new String[0]), selectedIndex, (dialog, which) -> {
                String theme = (which == 0) ? "light" : "dark";
                Config.set(KEY_THEME, theme);
                applyTheme();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showFontSizeSelection() {
        List<String> sizes = Arrays.asList("Small (12sp)", "Medium (14sp)", "Large (16sp)", "Extra Large (18sp)");
        int currentSize = Config.get(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        int selectedIndex = 1; // 默认中等大小
        
        // 根据当前设置确定选中的索引
        if (currentSize == 12) selectedIndex = 0;
        else if (currentSize == 16) selectedIndex = 2;
        else if (currentSize == 18) selectedIndex = 3;
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Select Font Size")
            .setSingleChoiceItems(sizes.toArray(new String[0]), selectedIndex, (dialog, which) -> {
                int size;
                switch (which) {
                    case 0: size = 12; break;
                    case 1: size = 14; break;
                    case 2: size = 16; break;
                    case 3: size = 18; break;
                    default: size = DEFAULT_FONT_SIZE;
                }
                Config.set(KEY_FONT_SIZE, size);
                applyFontSize();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showKeyBindingsSelection() {
        List<String> bindings = Arrays.asList("Default", "Vim", "Emacs");
        String currentBindings = Config.get(KEY_KEY_BINDINGS, DEFAULT_KEY_BINDINGS);
        int selectedIndex = 0; // 默认
        
        // 根据当前设置确定选中的索引
        if ("vim".equals(currentBindings)) selectedIndex = 1;
        else if ("emacs".equals(currentBindings)) selectedIndex = 2;
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Select Key Bindings")
            .setSingleChoiceItems(bindings.toArray(new String[0]), selectedIndex, (dialog, which) -> {
                String bindingsType;
                switch (which) {
                    case 0: bindingsType = "default"; break;
                    case 1: bindingsType = "vim"; break;
                    case 2: bindingsType = "emacs"; break;
                    default: bindingsType = DEFAULT_KEY_BINDINGS;
                }
                Config.set(KEY_KEY_BINDINGS, bindingsType);
                applyKeyBindings();
                Toast.makeText(this, "Key bindings changed to " + bindingsType, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
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