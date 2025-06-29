package com.manager.ssb.core.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LineNumberRecyclerView extends HorizontalScrollView {

    private RecyclerView recyclerView;
    private RecyclerView lineNumbersView;
    private TextAdapter textAdapter;
    private LineNumberAdapter lineNumberAdapter;

    public LineNumberRecyclerView(Context context) {
        super(context);
        init();
    }

    public LineNumberRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 创建水平滚动容器
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.HORIZONTAL);
        addView(container);
        
        // 行号视图
        lineNumbersView = new RecyclerView(getContext());
        lineNumbersView.setLayoutManager(new LinearLayoutManager(getContext()));
        lineNumbersView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(lineNumbersView);
        
        // 文本视图
        recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        container.addView(recyclerView);
        
        // 同步滚动
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lineNumbersView.scrollBy(0, dy);
            }
        });
    }
    
    public void setAdapters(TextAdapter textAdapter, LineNumberAdapter lineNumberAdapter) {
        this.textAdapter = textAdapter;
        this.lineNumberAdapter = lineNumberAdapter;
        recyclerView.setAdapter(textAdapter);
        lineNumbersView.setAdapter(lineNumberAdapter);
    }
    
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
    
    public void setTextSize(float size) {
        if (textAdapter != null) {
            textAdapter.setTextSize(size);
        }
    }
}