package com.manager.ssb.core.term;

import android.app.Activity;
import android.os.Bundle;

public class TermActivity extends Activity {
    private TermView termView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        termView = new TermView(this);
        setContentView(termView);

        // 自动弹出软键盘
        termView.requestFocus();
    }
}