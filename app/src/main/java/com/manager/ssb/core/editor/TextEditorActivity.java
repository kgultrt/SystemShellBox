package com.manager.ssb.core.editor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.manager.ssb.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextEditorActivity extends Activity {

    private static final String TAG = "TextEditor";
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB文件大小限制
    private static final int MAX_LINE_LENGTH = 5000; // 单行最大长度限制
    private static final int MAX_INITIAL_LINES = 500; // 初始加载行数
    private static final int LOAD_MORE_THRESHOLD = 20; // 加载更多阈值
    private static final int LOAD_CHUNK_SIZE = 200; // 每次加载行数
    
    // UI组件
    private TextView fileNameView;
    private TextView encodingView;
    private ProgressBar progressBar;
    private TextView longLineWarning;
    
    // 编辑器组件
    private LineNumberRecyclerView editorContainer;
    private TextAdapter textAdapter;
    private LineNumberAdapter lineNumberAdapter;
    
    // 文件信息
    private String filePath;
    private String fileName;
    private String detectedEncoding = "UTF-8";
    
    // 加载状态
    private long totalFileSize;
    private long bytesLoaded = 0;
    private int linesLoaded = 0;
    private boolean isLoadingComplete = false;
    private boolean isLongLinePresent = false;
    
    // 线程管理
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        // 初始化UI组件
        initViews();
        
        // 获取文件信息
        filePath = getIntent().getStringExtra("file_path");
        fileName = getIntent().getStringExtra("file_name");
        fileNameView.setText(fileName != null ? fileName : "Untitled");

        // 初始化编辑器
        initEditor();
        
        // 加载文件
        if (filePath != null) {
            loadText(filePath);
        } else {
            Toast.makeText(this, getString(R.string.cannot_open), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initViews() {
        fileNameView = findViewById(R.id.tv_file_name);
        encodingView = findViewById(R.id.tv_encoding);
        progressBar = findViewById(R.id.progress_bar);
        longLineWarning = findViewById(R.id.tv_long_line_warning);
        editorContainer = findViewById(R.id.editor_container);
        
        // 保存按钮
        ImageButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveText());
    }
    
    private void initEditor() {
        textAdapter = new TextAdapter();
        lineNumberAdapter = new LineNumberAdapter();
        editorContainer.setAdapters(textAdapter, lineNumberAdapter);
        
        // 设置滚动监听
        editorContainer.getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (isLoadingComplete || isLongLinePresent) return;
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                
                int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                int totalItems = layoutManager.getItemCount();
                
                // 当滚动到末尾附近时加载更多
                if (lastVisiblePosition >= totalItems - LOAD_MORE_THRESHOLD) {
                    loadMoreContent();
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    /* ================== 文件加载方法 ================== */
    private void loadText(String path) {
        showProgress(true);
        
        executor.execute(() -> {
            try {
                // 1. 获取文件基本信息
                File file = new File(path);
                totalFileSize = file.length();
                
                if (totalFileSize > MAX_FILE_SIZE) {
                    showError(getString(R.string.file_too_large));
                    return;
                }
                
                // 2. 探测编码
                detectedEncoding = detectEncoding(path);
                uiHandler.post(this::updateEncodingView);
                
                // 3. 加载初始内容
                loadInitialContent(path);
                
            } catch (Exception e) {
                Log.e(TAG, "加载文件出错", e);
                showError(getString(R.string.cannot_reading) + ": " + e.getMessage());
            } finally {
                showProgress(false);
            }
        });
    }
    
    /**
     * 使用文件开头部分探测编码
     */
    private String detectEncoding(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] buffer = new byte[4096]; // 4KB足够用于编码探测
            int bytesRead = fis.read(buffer);
            
            if (bytesRead <= 0) return "UTF-8";
            
            // 检查BOM
            if (bytesRead >= 3 && 
                (buffer[0] & 0xFF) == 0xEF && 
                (buffer[1] & 0xFF) == 0xBB && 
                (buffer[2] & 0xFF) == 0xBF) {
                return "UTF-8";
            }
            
            // 尝试UTF-8
            try {
                new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                return "UTF-8";
            } catch (Exception ignored) {}
            
            // 尝试GBK
            try {
                new String(buffer, 0, bytesRead, "GBK");
                return "GBK";
            } catch (Exception ignored) {}
            
            // 尝试ISO-8859-1
            return "ISO-8859-1";
        } catch (Exception e) {
            Log.e(TAG, "编码探测失败", e);
            return "UTF-8";
        }
    }
    
    /**
     * 加载初始可见内容
     */
    private void loadInitialContent(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), detectedEncoding))) {
            
            List<String> lines = new ArrayList<>();
            String line;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null && lineCount < MAX_INITIAL_LINES) {
                // 检查长行
                if (!isLongLinePresent && line.length() > MAX_LINE_LENGTH) {
                    isLongLinePresent = true;
                    uiHandler.post(this::showLongLineWarning);
                }
                
                lines.add(line);
                lineCount++;
                linesLoaded++;
                
                // 更新进度
                bytesLoaded += line.getBytes(detectedEncoding).length + 1; // +1 for newline
                updateProgress();
            }
            
            // 首次显示内容
            final List<String> finalLines = new ArrayList<>(lines);
            final int finalLineCount = lineCount;
            uiHandler.post(() -> {
                textAdapter.setLines(finalLines);
                lineNumberAdapter.setLineNumbers(1, finalLines.size());
                
                if (finalLineCount >= MAX_INITIAL_LINES) {
                    Toast.makeText(TextEditorActivity.this, 
                            getString(R.string.partial_load), Toast.LENGTH_SHORT).show();
                }
            });
            
            // 检查是否加载完成
            if (reader.readLine() == null) {
                isLoadingComplete = true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "加载初始内容失败", e);
            showError(getString(R.string.cannot_reading) + ": " + e.getMessage());
        }
    }
    
    /**
     * 滚动时加载更多内容
     */
    private void loadMoreContent() {
        if (isLoadingComplete) return;
        
        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), detectedEncoding))) {
                
                // 跳过已加载的行
                for (int i = 0; i < linesLoaded; i++) {
                    reader.readLine();
                }
                
                List<String> moreLines = new ArrayList<>();
                String line;
                int lineCount = 0;
                
                while ((line = reader.readLine()) != null && lineCount < LOAD_CHUNK_SIZE) {
                    // 检查长行
                    if (!isLongLinePresent && line.length() > MAX_LINE_LENGTH) {
                        isLongLinePresent = true;
                        uiHandler.post(this::showLongLineWarning);
                    }
                    
                    moreLines.add(line);
                    lineCount++;
                    linesLoaded++;
                    
                    // 更新进度
                    bytesLoaded += line.getBytes(detectedEncoding).length + 1; // +1 for newline
                    updateProgress();
                }
                
                // 追加内容
                final List<String> finalLines = new ArrayList<>(moreLines);
                final int finalLineCount = lineCount;
                uiHandler.post(() -> {
                    textAdapter.appendLines(finalLines);
                    lineNumberAdapter.appendLineNumbers(finalLines.size());
                    
                    if (finalLineCount >= LOAD_CHUNK_SIZE) {
                        Toast.makeText(TextEditorActivity.this, 
                                getString(R.string.loaded_more), Toast.LENGTH_SHORT).show();
                    }
                });
                
                // 检查是否加载完成
                if (reader.readLine() == null) {
                    isLoadingComplete = true;
                    uiHandler.post(() -> Toast.makeText(TextEditorActivity.this, 
                            getString(R.string.load_complete), Toast.LENGTH_SHORT).show());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "加载更多内容失败", e);
                showError(getString(R.string.load_error) + ": " + e.getMessage());
            }
        });
    }

    /* ================== UI更新方法 ================== */
    private void showProgress(boolean show) {
        uiHandler.post(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                progressBar.setProgress(0);
            }
        });
    }
    
    private void updateProgress() {
        if (totalFileSize <= 0) return;
        
        int progress = (int) ((bytesLoaded * 100) / totalFileSize);
        uiHandler.post(() -> progressBar.setProgress(progress));
    }
    
    private void updateEncodingView() {
        uiHandler.post(() -> encodingView.setText(detectedEncoding));
    }
    
    private void showLongLineWarning() {
        uiHandler.post(() -> {
            longLineWarning.setVisibility(View.VISIBLE);
            // 自动隐藏警告
            new Handler().postDelayed(() -> 
                longLineWarning.setVisibility(View.GONE), 5000);
        });
    }
    
    private void showError(String message) {
        uiHandler.post(() -> 
            Toast.makeText(TextEditorActivity.this, message, Toast.LENGTH_LONG).show()
        );
    }

    /* ------------------ 保存功能 ------------------ */
    private void saveText() {
        if (filePath == null) return;

        showProgress(true);
        
        executor.execute(() -> {
            try {
                // 获取所有文本行
                List<String> allLines = textAdapter.getAllLines();
                
                // 写入文件
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    for (String line : allLines) {
                        fos.write(line.getBytes(detectedEncoding));
                        fos.write("\n".getBytes(detectedEncoding));
                    }
                }
                
                uiHandler.post(() -> {
                    Toast.makeText(TextEditorActivity.this,
                            getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                    showProgress(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "保存文件失败", e);
                uiHandler.post(() -> {
                    Toast.makeText(TextEditorActivity.this,
                            getString(R.string.save_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showProgress(false);
                });
            }
        });
    }
}