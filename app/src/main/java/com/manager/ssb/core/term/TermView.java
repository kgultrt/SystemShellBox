package com.manager.ssb.core.term;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;

public class TermView extends View {

    private static final int[] ANSI_COLORS = new int[]{
            0xFF000000, // black
            0xFFAA0000, // red
            0xFF00AA00, // green
            0xFFAA5500, // yellow/brown
            0xFF0000AA, // blue
            0xFFAA00AA, // magenta
            0xFF00AAAA, // cyan
            0xFFAAAAAA, // light gray
    };

    private Cell[][] screen;
    private int rows, cols;

    private int cursorRow = 0, cursorCol = 0;
    private boolean cursorVisible = true;

    private int currentFg = 7; // light gray
    private int currentBg = 0; // black
    private boolean bold = false;

    private float charWidth, charHeight;
    private float scaleFactor = 1f;

    private Paint paintFg, paintBg;

    private InputMethodManager imm;

    private ScaleGestureDetector scaleGestureDetector;

    public TermView(Context context) {
        super(context);
        init(context);
    }

    public TermView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paintFg = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        paintFg.setColor(Color.LTGRAY);
        paintFg.setTypeface(Typeface.MONOSPACE);
        paintFg.setTextSize(24);
        paintFg.setFakeBoldText(false);

        paintBg = new Paint();
        paintBg.setColor(Color.BLACK);

        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));
                paintFg.setTextSize(24 * scaleFactor);
                requestLayout();  // 重新计算大小
                invalidate();
                return true;
            }
        });

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (charWidth == 0 || charHeight == 0) {
            charWidth = paintFg.measureText("M") * 1.1f;
            Paint.FontMetrics fm = paintFg.getFontMetrics();
            charHeight = (fm.bottom - fm.top + 4);
        }
        charWidth *= scaleFactor;
        charHeight *= scaleFactor;

        cols = Math.max(10, (int) Math.floor(width / charWidth));
        rows = Math.max(5, (int) Math.floor(height / charHeight));

        int newWidth = (int) (cols * charWidth);
        int newHeight = (int) (rows * charHeight);

        screen = new Cell[rows][cols];
        clearScreen();

        setMeasuredDimension(newWidth, newHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint.FontMetrics fm = paintFg.getFontMetrics();
        float baseLineShift = -fm.top + 2; // 字符基线微调，避免底部空白

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = screen[r][c];
                paintBg.setColor(ANSI_COLORS[cell.bg]);
                canvas.drawRect(
                        c * charWidth,
                        r * charHeight,
                        (c + 1) * charWidth,
                        (r + 1) * charHeight,
                        paintBg);

                paintFg.setColor(ANSI_COLORS[cell.fg]);
                paintFg.setFakeBoldText(cell.bold);

                float x = c * charWidth + 2;
                float y = r * charHeight + baseLineShift;
                canvas.drawText(String.valueOf(cell.ch), x, y, paintFg);
            }
        }

        if (cursorVisible && cursorRow < rows && cursorCol < cols) {
            paintBg.setColor(Color.WHITE);
            paintFg.setColor(Color.BLACK);

            float left = cursorCol * charWidth;
            float top = cursorRow * charHeight;
            float right = left + charWidth;
            float bottom = top + charHeight;

            canvas.drawRect(left, top, right, bottom, paintBg);

            Cell curCell = screen[cursorRow][cursorCol];
            paintFg.setFakeBoldText(curCell.bold);

            float cx = left + 2;
            float cy = top + baseLineShift;
            canvas.drawText(String.valueOf(curCell.ch), cx, cy, paintFg);
        }
    }

    public void clearScreen() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                screen[r][c] = new Cell(' ', currentFg, currentBg, bold);
            }
        }
        cursorRow = 0;
        cursorCol = 0;
        invalidate();
    }

    private void scrollUp() {
        for (int r = 1; r < rows; r++) {
            System.arraycopy(screen[r], 0, screen[r - 1], 0, cols);
        }
        for (int c = 0; c < cols; c++) {
            screen[rows - 1][c] = new Cell(' ', currentFg, currentBg, bold);
        }
        invalidate();
    }

    // 基础字符输入，支持换行、退格
    public void putChar(char ch) {
        switch (ch) {
            case '\n':
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= rows) {
                    scrollUp();
                    cursorRow = rows - 1;
                }
                break;
            case '\b':
                if (cursorCol > 0) {
                    cursorCol--;
                    screen[cursorRow][cursorCol] = new Cell(' ', currentFg, currentBg, bold);
                }
                break;
            default:
                screen[cursorRow][cursorCol] = new Cell(ch, currentFg, currentBg, bold);
                cursorCol++;
                if (cursorCol >= cols) {
                    cursorCol = 0;
                    cursorRow++;
                    if (cursorRow >= rows) {
                        scrollUp();
                        cursorRow = rows - 1;
                    }
                }
        }
        invalidate();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE;

        return new BaseInputConnection(this, false) {
            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                for (int i = 0; i < text.length(); i++) {
                    putChar(text.charAt(i));
                }
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        putChar('\b');
                        return true;
                    }
                }
                return super.sendKeyEvent(event);
            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理部分按键
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            putChar('\n');
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            putChar('\b');
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isFocused()) {
                requestFocus();
            }
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }

        return true;
    }

    private static class Cell {
        char ch;
        int fg;
        int bg;
        boolean bold;

        Cell(char ch, int fg, int bg, boolean bold) {
            this.ch = ch;
            this.fg = fg;
            this.bg = bg;
            this.bold = bold;
        }
    }
}