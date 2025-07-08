package com.manager.ssb.core.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.mrikso.codeeditor.lang.Language;
import com.mrikso.codeeditor.lang.LanguageJava;
import com.mrikso.codeeditor.util.Document;
import com.mrikso.codeeditor.util.DocumentProvider;
import com.mrikso.codeeditor.util.Lexer;
import com.mrikso.codeeditor.view.ColorScheme;
import com.mrikso.codeeditor.view.FreeScrollingTextField;
import com.mrikso.codeeditor.view.YoyoNavigationMethod;
import com.mrikso.codeeditor.view.autocomplete.AutoCompletePanel;

import com.manager.ssb.core.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextEditor extends FreeScrollingTextField {

    // 文档状态
    private boolean isEdited = false;
    
    // 文本变化监听器
    private final List<TextWatcher> textWatchers = new ArrayList<>();
    
    // 其他成员变量
    private Document _inputtingDoc;
    private boolean _isWordWrap;
    private Context mContext;
    private String _lastSelectFile;
    private int _index;
    private Toast toast;
    private float textSize;

    public TextEditor(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public TextEditor(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
        init();
    }

    private void init() {
        setVerticalScrollBarEnabled(true);
        setTypeface(Typeface.MONOSPACE);
        
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int configSize = Config.get("editor.font_size", 0);
        if (configSize == 0) {
            float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, dm);
        } else {
            float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, configSize, dm);
        }
        setTextSize((int) textSize);
        
        setShowLineNumbers(true);
        setHighlightCurrentRow(true);
        setWordWrap(false);
        setAutoComplete(true);
        setAutoIndent(true);
        setUseGboard(true);
        setAutoIndentWidth(2);
        
        
        setLanguage(LanguageJava.getInstance());
        setNavigationMethod(new YoyoNavigationMethod(this));
        
        // MD3 颜色方案
        setTextColor(Color.BLACK);
        setTextHighlightColor(Color.argb(255, 0, 120, 215));
        setBackgroundColor(Color.WHITE);
        setCommentColor(Color.GRAY);
        setKeywordColor(Color.BLUE);
        setBaseWordColor(Color.DKGRAY);
        setStringColor(Color.RED);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (_index != 0 && right > 0) {
            moveCaret(_index);
            _index = 0;
        }
    }

    public void setKeywordColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.KEYWORD, color);
    }

    public void setBaseWordColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.NAME, color);
    }

    public void setStringColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.STRING, color);
    }

    public void setCommentColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.COMMENT, color);
    }

    public void setBackgroundColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.BACKGROUND, color);
    }

    public void setTextColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.FOREGROUND, color);
    }

    public void setTextHighlightColor(int color) {
        getColorScheme().setColor(ColorScheme.Colorable.SELECTION_BACKGROUND, color);
    }

    public void setLanguage(Language language){
        AutoCompletePanel.setLanguage(language);
        Lexer.setLanguage(language);
    }

    public String getSelectedText() {
        return hDoc.subSequence(getSelectionStart(), getSelectionEnd() - getSelectionStart()).toString();
    }

    public void gotoLine(int line) {
        if (line > hDoc.getRowCount()) {
            line = hDoc.getRowCount();
        }
        int i = getText().getLineOffset(line - 1);
        setSelection(i);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        final int filteredMetaState = event.getMetaState() & ~KeyEvent.META_CTRL_MASK;
        if (KeyEvent.metaStateHasNoModifiers(filteredMetaState)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    selectAll();
                    return true;
                case KeyEvent.KEYCODE_X:
                    cut();
                    return true;
                case KeyEvent.KEYCODE_C:
                    copy();
                    return true;
                case KeyEvent.KEYCODE_V:
                    paste();
                    return true;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public void setWordWrap(boolean enable) {
        _isWordWrap = enable;
        super.setWordWrap(enable);
    }

    public DocumentProvider getText() {
        return createDocumentProvider();
    }

    public void setText(CharSequence c) {
        Document doc = new Document(this);
        doc.setWordWrap(_isWordWrap);
        doc.setText(c);
        setDocumentProvider(new DocumentProvider(doc));
        
        // 通知文本变化
        for (TextWatcher watcher : textWatchers) {
            watcher.onTextChanged(c, 0, c.length(), c.length());
        }
    }

    public File getOpenedFile() {
        if (_lastSelectFile != null)
            return new File(_lastSelectFile);

        return null;
    }

    public void setOpenedFile(String file) {
        _lastSelectFile = file;
    }

    public void insert(int idx, String text) {
        selectText(false);
        moveCaret(idx);
        paste(text);
    }

    public void replaceAll(CharSequence c) {
        replaceText(0, getLength() - 1, c.toString());
    }

    public void setSelection(int index) {
        selectText(false);
        if (!hasLayout())
            moveCaret(index);
        else
            _index = index;
    }

    public void undo() {
        DocumentProvider doc = createDocumentProvider();
        int newPosition = doc.undo();

        if (newPosition >= 0) {
            setEdited(true);
            respan();
            selectText(false);
            moveCaret(newPosition);
            invalidate();
        }
    }

    public void redo() {
        DocumentProvider doc = createDocumentProvider();
        int newPosition = doc.redo();

        if (newPosition >= 0) {
            setEdited(true);
            respan();
            selectText(false);
            moveCaret(newPosition);
            invalidate();
        }
    }
    
    public void addTextChangedListener(TextWatcher watcher) {
        if (!textWatchers.contains(watcher)) {
            textWatchers.add(watcher);
        }
    }
    
    public void removeTextChangedListener(TextWatcher watcher) {
        textWatchers.remove(watcher);
    }
    
    public boolean isEdited() {
        return isEdited;
    }
    
    public void setEdited(boolean edited) {
        this.isEdited = edited;
    }
    
    @Override
    public void replaceText(int start, int length, String text) {
        super.replaceText(start, length, text);
        
        for (TextWatcher watcher : textWatchers) {
            watcher.onTextChanged(text, start, length, text.length());
        }
    }

    private void showToast(CharSequence text) {
        if (toast == null) {
            toast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
        }
        toast.show();
    }
}