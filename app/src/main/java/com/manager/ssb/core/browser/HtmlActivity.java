package com.manager.ssb.core.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.manager.ssb.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class HtmlActivity extends AppCompatActivity {

    private static final String TAG = "HtmlWebView";
    private WebView webView;
    private ProgressBar progressBar;
    private EditText urlBar;
    private TextView titleBar;
    private LinearLayout controlsLayout;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomLayout();
        
        // 设置 WebView 并加载内容
        configureWebView();
        loadHtmlFile();
    }

private void createCustomLayout() {
    // 创建主容器
    RelativeLayout rootLayout = new RelativeLayout(this);
    rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
    
    // 1. 紧凑型顶部控制栏
    LinearLayout topBar = new LinearLayout(this);
    topBar.setId(View.generateViewId());
    topBar.setOrientation(LinearLayout.HORIZONTAL);
    topBar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.background_dark));
    
    RelativeLayout.LayoutParams topBarParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT,
        (int) getResources().getDimension(R.dimen.compact_bar_height)); // 使用紧凑高度
    topBar.setLayoutParams(topBarParams);
    
    // 2. 后退按钮
    ImageButton backButton = createCompactButton(R.drawable.ic_arrow_back, v -> {
        if (webView.canGoBack()) webView.goBack();
    });
    topBar.addView(backButton);
    
    // 3. 前进按钮
    ImageButton forwardButton = createCompactButton(R.drawable.ic_arrow_forward, v -> {
        if (webView.canGoForward()) webView.goForward();
    });
    topBar.addView(forwardButton);
    
    // 4. 刷新按钮
    ImageButton refreshButton = createCompactButton(R.drawable.ic_refresh, v -> webView.reload());
    topBar.addView(refreshButton);
    
    // 5. 停止按钮
    ImageButton stopButton = createCompactButton(R.drawable.ic_close, v -> webView.stopLoading());
    topBar.addView(stopButton);
    
    // 6. 主页按钮
    ImageButton homeButton = createCompactButton(R.drawable.ic_home, v -> loadHtmlFile());
    topBar.addView(homeButton);
    
    // 7. 紧凑型URL地址栏
    urlBar = new EditText(this);
    urlBar.setSingleLine(true);
    urlBar.setBackgroundResource(android.R.drawable.edit_text);
    urlBar.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    urlBar.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
    urlBar.setHint(getString(R.string.input_website_hint));
    
    LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
        0, // 宽度自适应
        ViewGroup.LayoutParams.MATCH_PARENT);
    urlParams.weight = 1; // 占据剩余空间
    urlParams.setMargins(4, 4, 4, 4);
    urlBar.setLayoutParams(urlParams);
    topBar.addView(urlBar);
    
    // 8. 紧凑型进度条
    progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progressBar.setLayoutParams(new LinearLayout.LayoutParams(
        (int) getResources().getDimension(R.dimen.progress_bar_width),
        ViewGroup.LayoutParams.MATCH_PARENT));
    progressBar.setMax(100);
    progressBar.setVisibility(View.GONE);
    topBar.addView(progressBar);
    
    // 9. 创建 WebView
    webView = new WebView(this);
    RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT,
        RelativeLayout.LayoutParams.MATCH_PARENT);
    webViewParams.addRule(RelativeLayout.BELOW, topBar.getId());
    webView.setLayoutParams(webViewParams);
    
    // 10. 全屏容器（用于视频全屏播放）
    fullscreenContainer = new FrameLayout(this);
    fullscreenContainer.setLayoutParams(new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT));
    fullscreenContainer.setVisibility(View.GONE);
    
    // 添加到根布局
    rootLayout.addView(topBar);
    rootLayout.addView(webView);
    rootLayout.addView(fullscreenContainer);
    
    setContentView(rootLayout);
}

// 创建紧凑型按钮的辅助方法
private ImageButton createCompactButton(int iconRes, View.OnClickListener listener) {
    ImageButton button = new ImageButton(this);
    button.setImageResource(iconRes);
    button.setBackground(null);
    button.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
    button.setOnClickListener(listener);
    
    // 紧凑尺寸
    int buttonSize = (int) getResources().getDimension(R.dimen.compact_button_size);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        buttonSize,
        buttonSize);
    params.setMargins(2, 2, 2, 2);
    button.setLayoutParams(params);
    
    return button;
}



    private void addControlButton(int iconRes, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setBackground(null);
        button.setOnClickListener(listener);
        button.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT);
        params.weight = 1;
        button.setLayoutParams(params);
        
        controlsLayout.addView(button);
    }

    @SuppressLint({"RequiresFeature", "JavascriptInterface"})
    private void configureWebView() {
        // 1. 启用基本功能
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // 启用 DOM 存储 (localStorage)
        settings.setDomStorageEnabled(true);
    
        // 启用数据库支持
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(getDatabasePath("webview_db").getPath());
        
        // 2. 高级渲染设置
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        
        // 3. 安全设置增强
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        
        // 4. 兼容性缓存策略
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabasePath(getCacheDir().getPath());
        // 替代 AppCache 的现代缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(getCacheDir().getPath());
        
        // 5. 高级Web API支持
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false); // 允许自动播放媒体
        
        // 6. 支持新HTML5功能
        settings.setSaveFormData(true);
        settings.setTextZoom(100);
        
        // 7. 增强的设备兼容性
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, true);
            }
        }
        
        // 8. WebView客户端配置
        webView.setWebViewClient(new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoadingImpl(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldOverrideUrlLoadingImpl(url);
            }
            
            private boolean shouldOverrideUrlLoadingImpl(String url) {
                // 处理特殊协议
                if (url.startsWith("intent:") || 
                    url.startsWith("market:") ||
                    url.startsWith("whatsapp:")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                            return true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Intent URI parsing error", e);
                    }
                    return true;
                }
                
                webView.loadUrl(url);
                urlBar.setText(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                urlBar.setText(url);
                //titleBar.setText(view.getTitle());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                progressBar.setProgress(100);
                urlBar.setText(url);
                //titleBar.setText(view.getTitle());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                displayError("加载失败: " + error.getDescription());
            }

            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    displayError("HTTP错误: " + errorResponse.getStatusCode());
                } else {
                    displayError("HTTP错误");
                }
            }
        });
        
        // 9. WebChrome客户端（处理进度、权限、全屏等）
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress > 0 && newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                //titleBar.setText(title);
                if (title != null && !title.isEmpty()) {
                    setTitle(title);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // 处理所有权限请求
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // 在这里实现文件选择器支持（简化版）
                Toast.makeText(HtmlActivity.this, "文件选择功能需要额外实现", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // 进入全屏模式
                if (webView != null) {
                    webView.setVisibility(View.GONE);
                }
                fullscreenContainer.setVisibility(View.VISIBLE);
                fullscreenContainer.addView(view);
                customViewCallback = callback;
            }

            @Override
            public void onHideCustomView() {
                // 退出全屏模式
                if (webView != null) {
                    webView.setVisibility(View.VISIBLE);
                }
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
                fullscreenContainer.setVisibility(View.GONE);
                fullscreenContainer.removeAllViews();
            }
        });
        
        // 10. 键盘事件处理
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromBar();
                return true;
            }
            return false;
        });
        
        // 11. JavaScript接口桥接
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        
        // 12. 启用地理位置支持
        settings.setGeolocationEnabled(true);
        
        // 13. Cookie管理
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        
        // 14. 现代Web功能支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, true);
            }
        }
        
        // 15. WebGL和硬件加速支持
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        
        // 16. 高级调试选项
        if (isAppDebuggable()) {
            // 在API 19+上启用WebView调试
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
        
        // 17. 提高渲染优先级
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }
    }
    
    private boolean isAppDebuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public void displayError(String error) {
        String htmlError = "<html><body style='background:#f0f0f0;padding:40px;font-family:sans-serif'>" +
                           "<h2>网页加载失败</h2>" +
                           "<p>" + error + "</p>" +
                           "<button style='padding:10px 20px;background:#007bff;color:white;border:none;border-radius:4px;' " +
                           "onclick='location.reload()'>重新加载</button>" +
                           "</body></html>";
        webView.loadDataWithBaseURL(null, htmlError, "text/html", "UTF-8", null);
        progressBar.setVisibility(View.GONE);
    }

    private void loadHtmlFile() {
        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null && !filePath.isEmpty()) {
            if (URLUtil.isFileUrl(filePath) || URLUtil.isContentUrl(filePath)) {
                webView.loadUrl(filePath);
            } else if (URLUtil.isNetworkUrl(filePath)) {
                webView.loadUrl(filePath);
            } else {
                webView.loadUrl("file://" + filePath);
            }
            urlBar.setText(filePath);
        } else {
            // 默认加载本地html示例
            loadLocalResource("sample.html");
        }
    }
    
    private void loadLocalResource(String filename) {
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String html = new String(buffer, "UTF-8");
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        } catch (IOException e) {
            displayError("找不到默认页面: " + e.getMessage());
        }
    }
    
    private void loadUrlFromBar() {
        String url = urlBar.getText().toString().trim();
        
        // 添加URL方案处理
        if (URLUtil.isValidUrl(url)) {
            webView.loadUrl(url);
            return;
        }
        
        // 尝试作为本地文件加载
        if (url.startsWith("/")) {
            webView.loadUrl("file://" + url);
            return;
        }
        
        // 尝试添加协议
        String[] protocols = {"http://", "https://", "file://"};
        for (String protocol : protocols) {
            String testUrl = protocol + url;
            if (URLUtil.isValidUrl(testUrl)) {
                webView.loadUrl(testUrl);
                return;
            }
        }
        
        // 加载纯HTML内容作为后备
        webView.loadData("<html><body><h1>加载失败</h1><p>无法解析的地址: " + url + "</p></body></html>", 
                         "text/html", "UTF-8");
    }

    @Override
    public void onBackPressed() {
        if (fullscreenContainer.getVisibility() == View.VISIBLE) {
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    // JavaScript接口桥接类
    public static class JavaScriptInterface {
        private final Context context;

        public JavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}