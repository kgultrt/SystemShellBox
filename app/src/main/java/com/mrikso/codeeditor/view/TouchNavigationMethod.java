package com.mrikso.codeeditor.view;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.mrikso.codeeditor.util.DocumentProvider;

//TODO minimise unnecessary invalidate calls

/**
 * TouchNavigationMethod classes implementing their own carets have to override
 * getCaretBloat() to return the size of the drawing area it needs, in excess of
 * the bounding box of the character the caret is on, and use
 * onTextDrawComplete(Canvas) to draw the caret. Currently, only a fixed size
 * caret is allowed, but scalable carets may be implemented in future.
 */
public class TouchNavigationMethod extends GestureDetector.SimpleOnGestureListener {
    private final static Rect mCaretBloat = new Rect(0, 0, 0, 0);
    // When the caret is dragged to the edges of the text field, the field will
    // scroll automatically. SCROLL_EDGE_SLOP is the width of these edges in pixels
    // and extends inside the content area, not outside to the padding area
    //protected static int SCROLL_EDGE_SLOP = 100;
    /**
     * The radius, in density-independent pixels, around a point of interest
     * where any touch event within that radius is considered to have touched
     * the point of interest itself
     */
    protected static int TOUCH_SLOP = 12;
    protected FreeScrollingTextField mTextField;
    protected boolean isCaretTouched = false;
    private GestureDetector mGestureDetector;
    private float lastDist, lastSize;
    private float lastX, lastY;
    private int fling;

    public TouchNavigationMethod(FreeScrollingTextField textField) {
        mTextField = textField;
        mGestureDetector = new GestureDetector(textField.getContext(), this);
        mGestureDetector.setIsLongpressEnabled(true);
    }

    @SuppressWarnings("unused")
    private TouchNavigationMethod() {
        // do not invoke; always needs a valid mTextField
    }

    @Override
    public boolean onDown(MotionEvent e) {
       // mTextField.getParent().requestDisallowInterceptTouchEvent(e.getX() > 10);
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        isCaretTouched = isNearChar(x, y, mTextField.getCaretPosition());

        if (mTextField.isFlingScrolling()) {
            mTextField.stopFlingScrolling();
        } else if (mTextField.isSelectText()) {
            if (isNearChar(x, y, mTextField.getSelectionStart())) {
                mTextField.focusSelectionStart();
                mTextField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                isCaretTouched = true;
            } else if (isNearChar(x, y, mTextField.getSelectionEnd())) {
                mTextField.focusSelectionEnd();
                mTextField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                isCaretTouched = true;
            }
        }

        if (isCaretTouched) {
            mTextField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // do nothing
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = mTextField.coordToCharIndex(x, y);

        if (mTextField.isSelectText()) {
            int strictCharOffset = mTextField.coordToCharIndexStrict(x, y);
            if (mTextField.inSelectionRange(strictCharOffset) ||
                    isNearChar(x, y, mTextField.getSelectionStart()) ||
                    isNearChar(x, y, mTextField.getSelectionEnd())) {
                // do nothing
            } else {
                mTextField.selectText(false);
                if (charOffset >= 0) {
                    mTextField.moveCaret(charOffset);
                }
            }
        } else {
            if (charOffset >= 0) {
                mTextField.moveCaret(charOffset);
            }
        }
        mTextField.showIME(true);
        return true;
    }

    /**
     * Note that up events from a fling are NOT captured here.
     * Subclasses have to call super.onUp(MotionEvent) in their implementations
     * of onFling().
     * <p>
     * Also, up events from non-primary pointers in a multi-touch situation are
     * not captured here.
     *
     * @param e
     * @return
     */
    public boolean onUp(MotionEvent e) {
        mTextField.stopAutoScrollCaret();
        isCaretTouched = false;
        lastDist = 0;
        fling = 0;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        //onTouchZoon(e2);

        if (isCaretTouched) {
            dragCaret(e2);
        } else if (e2.getPointerCount() == 1) {
            if (fling == 0)
                if (Math.abs(distanceX) > Math.abs(distanceY))
                    fling = 1;
                else
                    fling = -1;
            if (fling == 1)
                distanceY = 0;
            else if (fling == -1)
                distanceX = 0;

            scrollView(distanceX, distanceY);
        }

        //TODO find out if ACTION_UP events are actually passed to onScroll
        if ((e2.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            onUp(e2);
        }
        return true;
    }

    protected void dragCaret(MotionEvent e) {
        if (!mTextField.isSelectText() && isDragSelect()) {
            mTextField.selectText(true);
        }

        int x = (int) e.getX() - mTextField.getPaddingLeft();
        int y = (int) e.getY() - mTextField.getPaddingTop();
        boolean scrolled = false;

        // If the edges of the textField content area are touched, scroll in the
        // corresponding direction.
        if (x < mTextField.SCROLL_EDGE_SLOP) {
            scrolled = mTextField.autoScrollCaret(FreeScrollingTextField.SCROLL_LEFT);
        } else if (x >= (mTextField.getContentWidth() - mTextField.SCROLL_EDGE_SLOP)) {
            scrolled = mTextField.autoScrollCaret(FreeScrollingTextField.SCROLL_RIGHT);
        } else if (y < mTextField.SCROLL_EDGE_SLOP) {
            scrolled = mTextField.autoScrollCaret(FreeScrollingTextField.SCROLL_UP);
        } else if (y >= (mTextField.getContentHeight() - mTextField.SCROLL_EDGE_SLOP)) {
            scrolled = mTextField.autoScrollCaret(FreeScrollingTextField.SCROLL_DOWN);
        }

        if (!scrolled) {
            mTextField.stopAutoScrollCaret();
            int newCaretIndex = mTextField.coordToCharIndex(
                    screenToViewX((int) e.getX()),
                    screenToViewY((int) e.getY())
            );
            if (newCaretIndex >= 0) {
                mTextField.moveCaret(newCaretIndex);
            }
        }
    }

    private void scrollView(float distanceX, float distanceY) {
        int newX = (int) distanceX + mTextField.getScrollX();
        int newY = (int) distanceY + mTextField.getScrollY();

        // If scrollX and scrollY are somehow more than the recommended
        // max scroll values, use them as the new maximum
        // Also take into account the size of the caret,
        // which may extend beyond the text boundaries
        int maxWidth = Math.max(mTextField.getMaxScrollX(), mTextField.getScrollX());
        if (newX > maxWidth) {
            newX = maxWidth;
        } else if (newX < 0) {
            newX = 0;
        }

        int maxHeight = Math.max(mTextField.getMaxScrollY(), mTextField.getScrollY());
        if (newY > maxHeight) {
            newY = maxHeight;
        } else if (newY < 0) {
            newY = 0;
        }
        //mTextField.scrollTo(newX, newY);
        mTextField.smoothScrollTo(newX, newY);

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!isCaretTouched) {

            if (fling == 1)
                velocityY = 0;
            else if (fling == -1)
                velocityX = 0;

            mTextField.flingScroll((int) -velocityX, (int) -velocityY);
        }
        onUp(e2);
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private boolean onTouchZoom(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            if (e.getPointerCount() == 2) {
                if (lastDist == 0) {
                    float x = e.getX(0) - e.getX(1);
                    float y = e.getY(0) - e.getY(1);
                    lastDist = (float) Math.sqrt(x * x + y * y);
                    lastX = (e.getX(0) + e.getX(1)) / 2;
                    lastY = (e.getY(0) + e.getY(1)) / 2;
                    lastSize = mTextField.getTextSize();
                }

                float dist = spacing(e);
                if (lastDist != 0) {
                    mTextField.setTextSize((int) (lastSize * (dist / lastDist)));
                    //mTextField.scrollBy(0,(int)(lastY-lastY*(mTextField.getTextSize() / lastSize)));
                }
                mTextField.setTextSize((int)(mTextField.getTextSize() * dist / lastDist));
                //lastDist = dist;
                return true;
            }
        }
        lastDist = 0;
        return false;
    }

    /**
     * Subclasses overriding this method have to call the superclass method
     */
    public boolean onTouchEvent(MotionEvent event) {
        onTouchZoom(event);
        boolean handled = mGestureDetector.onTouchEvent(event);
        if (!handled
                && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            // propagate up events since GestureDetector does not do so
            handled = onUp(event);
        }
        return handled;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
         isCaretTouched = true;
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = mTextField.coordToCharIndex(x, y);

        if (mTextField.isSelectText() && mTextField.inSelectionRange(charOffset)) {
            DocumentProvider doc = mTextField.createDocumentProvider();
            int line = doc.findLineNumber(charOffset);
            int start = doc.getLineOffset(line);
            int end = doc.getLineOffset(line+1)-1;
            mTextField.setSelectionRange(start, end - start);
        } else {
            if (charOffset >= 0) {
                mTextField.moveCaret(charOffset);
                DocumentProvider doc = mTextField.createDocumentProvider();
                int start;
                int end;
                for (start = charOffset; start >= 0; start--) {
                    char c = doc.charAt(start);
                    if (!Character.isJavaIdentifierPart(c))
                        break;
                }
                if (start != charOffset)
                    start++;
                for (end = charOffset; end >= 0; end++) {
                    char c = doc.charAt(end);
                    if (!Character.isJavaIdentifierPart(c))
                        break;
                }
                mTextField.selectText(true);
                mTextField.setSelectionRange(start, end - start);
            }
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Android lifecyle event. See {@link android.app.Activity#onPause()}.
     */
    void onPause() {
        //do nothing
    }

    /**
     * Android lifecyle event. See {@link android.app.Activity#onResume()}.
     */
    void onResume() {
        //do nothing
    }

    /**
     * Called by FreeScrollingTextField when it has finished drawing text.
     * Classes extending TouchNavigationMethod can use this to draw, for
     * example, a custom caret.
     * <p>
     * The canvas includes padding in it.
     *
     * @param canvas
     */
    public void onTextDrawComplete(Canvas canvas) {
        // Do nothing. Basic caret drawing is handled by FreeScrollingTextField.
    }

    public void onColorSchemeChanged(ColorScheme colorScheme) {
        // Do nothing. Derived classes can use this to change their graphic assets accordingly.
    }


    //*********************************************************************
    //**************************** Utilities ******************************
    //*********************************************************************

    public void onChiralityChanged(boolean isRightHanded) {
        // Do nothing. Derived classes can use this to change their input
        // handling and graphic assets accordingly.
    }

    /**
     * For any printed character, this method returns the amount of space
     * required in excess of the bounding box of the character to draw the
     * caret.
     * Subclasses should override this method if they are drawing their
     * own carets.
     */
    public Rect getCaretBloat() {
        return mCaretBloat;
    }

    final protected int getPointerId(MotionEvent e) {
        return (e.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                >> MotionEvent.ACTION_POINTER_ID_SHIFT;
    }

    /**
     * Converts a x-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    final protected int screenToViewX(int x) {
        return x - mTextField.getPaddingLeft() + mTextField.getScrollX();
    }

    /**
     * Converts a y-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    final protected int screenToViewY(int y) {
        return y - mTextField.getPaddingTop() + mTextField.getScrollY();
    }

    final public boolean isRightHanded() {
        return true;
    }

    final private boolean isDragSelect() {
        return false;
    }

    /**
     * Determine if a point(x,y) on screen is near a character of interest,
     * specified by its index charOffset. The radius of proximity is defined
     * by TOUCH_SLOP.
     *
     * @param x          X-coordinate excluding padding
     * @param y          Y-coordinate excluding padding
     * @param charOffset the character of interest
     * @return Whether (x,y) lies close to the character with index charOffset
     */
    public boolean isNearChar(int x, int y, int charOffset) {
        Rect bounds = mTextField.getBoundingBox(charOffset);

        return (y >= (bounds.top - TOUCH_SLOP)
                && y < (bounds.bottom + TOUCH_SLOP)
                && x >= (bounds.left - TOUCH_SLOP)
                && x < (bounds.right + TOUCH_SLOP)
        );
    }
}
