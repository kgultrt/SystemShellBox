/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
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

package com.manager.ssb.core.editor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.EventReceiver;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.SymbolInputView;

import com.manager.ssb.R;
import com.manager.ssb.core.settings.SettingsActivity;
import com.manager.ssb.core.config.Config;

public class MainActivity extends AppCompatActivity {
    
    // 编辑器组件
    private CodeEditor codeEditor;
    private SymbolInputView symbolInputView;
    private Toolbar toolbar;
    // private TextView ";
    // private TextView textViewFileInfo;
    
    // 文件相关
    private String currentFilePath;
    private String currentFileName;
    private boolean isFileModified = false;
    private long lastModifiedTime = 0;
    
    
    // 菜单项
    private MenuItem undoMenuItem;
    private MenuItem redoMenuItem;
    private MenuItem saveMenuItem;
    
    // 符号定义
    private static final String[] SYMBOLS = new String[] {
        "{", "}", "(", ")", "[", "]",
        "<", ">", "=", "+", "-", "*", "/",
        ";", ":", "'", "\"", "\\", "|",
        "&", "!", "?", "@", "#", "$", "%",
        "~", "^", "`"
    };
    
    private static final String[] SYMBOL_INSERT_TEXT = new String[] {
        "{}", "}", "(", ")", "[]", "]",
        "<>", ">", "=", "+", "-", "*", "/",
        ";", ":", "'", "\"\"", "\\", "|",
        "&&", "!", "?", "@", "#", "$", "%",
        "~", "^", "`"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texteditor);
        
        initializeViews();
        initializeEditor();
        loadFileFromIntent();
        setupAutoSave();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        
        codeEditor = findViewById(R.id.code_editor);
        symbolInputView = findViewById(R.id.symbol_input);
        // textViewLineColumn = findViewById(R.id.text_line_column);
        // textViewFileInfo = findViewById(R.id.text_file_info);
        
    }
    
    private void initializeEditor() {
        // 基本设置
        codeEditor.setEditable(true);
        codeEditor.setLineNumberEnabled(true);
        codeEditor.setWordwrap(Config.get("editor.word_wrap", false));
        codeEditor.setTextSize(Config.get("font_size", 14));
        codeEditor.setTypefaceText(Typeface.MONOSPACE);
        codeEditor.setLineSpacing(2.0f, 1.1f);
        
        // 符号输入
        symbolInputView.bindEditor(codeEditor);
        symbolInputView.addSymbols(SYMBOLS, SYMBOL_INSERT_TEXT);
        
        // 设置语言
        setEditorLanguage();
        
        // 应用保存的设置
        applyEditorSettings();
        
        // 事件监听
        setupEventListeners();
    }
    
    private void setEditorLanguage() {
        // 检查文件扩展名，决定使用哪种语言
        if (currentFileName != null) {
            if (currentFileName.endsWith(".java")) {
                codeEditor.setEditorLanguage(new JavaLanguage());
            } else {
                // 其他文件类型使用空语言
                codeEditor.setEditorLanguage(new EmptyLanguage());
            }
        } else {
            // 默认使用空语言
            codeEditor.setEditorLanguage(new EmptyLanguage());
        }
    }
    
    private void setupEventListeners() {
        // 内容变化监听
        codeEditor.subscribeEvent(ContentChangeEvent.class, 
            (event, unsubscribe) -> {
                isFileModified = true;
                updateUndoRedoState();
                updateFileStatus();
            });
        
        // 选择变化监听
        codeEditor.subscribeEvent(SelectionChangeEvent.class,
            (event, unsubscribe) -> {
                int line = codeEditor.getCursor().getLeftLine() + 1;
                int column = codeEditor.getCursor().getLeftColumn() + 1;
                String positionText = String.format("Ln %d, Col %d", line, column);
                
                if (codeEditor.getCursor().isSelected()) {
                    int selectedLength = codeEditor.getCursor().getRight() - 
                                        codeEditor.getCursor().getLeft();
                    positionText += String.format(" (%d selected)", selectedLength);
                }
                
                // textViewLineColumn.setText(positionText);
            });
    }
    
    private void applyEditorSettings() {
        // 应用字体大小
        int fontSize = Config.get("editor.font_size", 14);
        codeEditor.setTextSize(fontSize);
        
        // 应用自动换行
        boolean wordWrap = Config.get("editor.word_wrap", false);
        codeEditor.setWordwrap(wordWrap);
        
        // 应用显示行号
        boolean showLineNumbers = Config.get("editor.show_line_numbers", true);
        codeEditor.setLineNumberEnabled(showLineNumbers);
        
        // 应用代码补全
        boolean autoComplete = Config.get("editor.auto_complete", true);
        codeEditor.getComponent(io.github.rosemoe.sora.widget.component.EditorAutoCompletion.class)
                  .setEnabled(autoComplete);
    }
    
    private void loadFileFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("file_path")) {
            currentFilePath = intent.getStringExtra("file_path");
            File file = new File(currentFilePath);
            currentFileName = file.getName();
            
            // 设置标题
            setTitle(currentFileName);
            // textViewFileInfo.setText(file.getParent());
            
            // 加载文件内容
            loadFileContent();
            
            // 重新设置语言（因为现在知道文件名了）
            setEditorLanguage();
        } else {
            setTitle("Untitled");
            // textViewFileInfo.setText("New file");
            codeEditor.setText("");
        }
    }
    
    private void loadFileContent() {
        if (currentFilePath != null) {
            new Thread(() -> {
                try {
                    File file = new File(currentFilePath);
                    lastModifiedTime = file.lastModified();
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            new FileInputStream(file),
                            StandardCharsets.UTF_8
                        )
                    );
                    
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();
                    
                    runOnUiThread(() -> {
                        codeEditor.setText(content.toString());
                        isFileModified = false;
                        updateFileStatus();
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        showSnackbar("Failed to load file: " + e.getMessage());
                        codeEditor.setText("");
                    });
                }
            }).start();
        }
    }
    
    private void setupAutoSave() {
        // 每分钟自动保存
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFileModified && currentFilePath != null) {
                    saveFile(false);
                }
                handler.postDelayed(this, 60000); // 每分钟检查一次
            }
        };
        handler.postDelayed(autoSaveRunnable, 60000);
    }
    
    private void updateUndoRedoState() {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(codeEditor.canUndo());
        }
        if (redoMenuItem != null) {
            redoMenuItem.setEnabled(codeEditor.canRedo());
        }
        if (saveMenuItem != null) {
            saveMenuItem.setEnabled(isFileModified);
        }
    }
    
    private void updateFileStatus() {
        // String status = currentFileName != null ? currentFileName : "Untitled";
        // if (isFileModified) {
            // status += " *";
        // }
        // textViewFileInfo.setText(status);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        
        undoMenuItem = menu.findItem(R.id.menu_undo);
        redoMenuItem = menu.findItem(R.id.menu_redo);
        saveMenuItem = menu.findItem(R.id.menu_save);
        
        updateUndoRedoState();
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_save) {
            saveFile(true);
            return true;
            
        } else if (id == R.id.menu_undo) {
            codeEditor.undo();
            return true;
            
        } else if (id == R.id.menu_redo) {
            codeEditor.redo();
            return true;
            
        } else if (id == R.id.menu_find) {
            showFindDialog();
            return true;
            
        } else if (id == R.id.menu_replace) {
            showReplaceDialog();
            return true;
            
        } else if (id == R.id.menu_goto) {
            showGoToDialog();
            return true;
            
        } else if (id == R.id.menu_format) {
            formatCode();
            return true;
            
        } else if (id == R.id.menu_settings) {
            openSettings();
            return true;
            
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void saveFile(boolean showMessage) {
        if (currentFilePath == null) {
            // 如果是新文件，需要先选择保存位置
            showSaveAsDialog();
            return;
        }
        
        new Thread(() -> {
            try {
                String content = codeEditor.getText().toString();
                FileOutputStream fos = new FileOutputStream(currentFilePath);
                fos.write(content.getBytes(StandardCharsets.UTF_8));
                fos.close();
                
                lastModifiedTime = new File(currentFilePath).lastModified();
                isFileModified = false;
                
                runOnUiThread(() -> {
                    updateFileStatus();
                    updateUndoRedoState();
                    if (showMessage) {
                        showSnackbar("File saved successfully");
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showSnackbar("Failed to save file: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void showSaveAsDialog() {
        // 这里可以实现文件保存对话框
        showSnackbar("Save As feature not implemented yet");
    }
    
    private void showFindDialog() {
        // 实现查找对话框
        showSnackbar("Find feature not implemented yet");
    }
    
    private void showReplaceDialog() {
        // 实现替换对话框
        showSnackbar("Replace feature not implemented yet");
    }
    
    private void showGoToDialog() {
        // 实现跳转到行对话框
        showSnackbar("Go To Line feature not implemented yet");
    }
    
    private void formatCode() {
        // 简单的代码格式化
        if (currentFileName != null && currentFileName.endsWith(".java")) {
            formatJavaCode();
        } else {
            showSnackbar("Formatting only supported for Java files");
        }
    }
    
    private void formatJavaCode() {
        // 简单的Java代码格式化
        // 这里可以实现代码格式化逻辑
        showSnackbar("Java formatting not fully implemented");
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, 1);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 重新应用设置
            applyEditorSettings();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isFileModified) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }
    
    private void showUnsavedChangesDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save before exiting?")
            .setPositiveButton("Save", (dialog, which) -> {
                saveFile(false);
                finish();
            })
            .setNegativeButton("Don't Save", (dialog, which) -> finish())
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (codeEditor != null) {
            codeEditor.release();
        }
    }
}