package com.mrikso.codeeditor.view;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;

import com.mrikso.codeeditor.lang.Language;
import com.mrikso.codeeditor.util.DLog;
import com.mrikso.codeeditor.util.Lexer;

import static com.mrikso.codeeditor.util.DLog.log;

//*********************************************************************
//************************** InputConnection **************************
//*********************************************************************
/*
 * Does not provide ExtractedText related methods
 */
public class TextFieldInputConnection extends BaseInputConnection {
    private boolean isComposing = false;
    private int composingCharCount = 0;
    private int mCaretPosition = -1;
    private FreeScrollingTextField textField;

    public TextFieldInputConnection(FreeScrollingTextField v ) {
        super(v, true);
        textField = v;
    }

    public void resetComposingState() {
        composingCharCount = 0;
        isComposing = false;
        textField.hDoc.endBatchEdit();
    }

    @Override
    public boolean performContextMenuAction(int id) {
        switch (id) {
            case android.R.id.copy:
                textField.copy();
                break;
            case android.R.id.cut:
                textField.cut();
                break;
            case android.R.id.paste:
                textField.paste();
                break;
            case android.R.id.startSelectingText:
            case android.R.id.stopSelectingText:
            case android.R.id.selectAll:
                textField.selectAll();
                break;
        }

        return false;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                if (textField.isSelectText())
                    textField.selectText(false);
                else
                    textField.selectText(true);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                textField.moveCaretLeft();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                textField.moveCaretUp();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                textField.moveCaretRight();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                textField.moveCaretDown();
                break;
            case KeyEvent.KEYCODE_MOVE_HOME:
                textField.moveCaret(0);
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                textField.moveCaret(textField.hDoc.length() - 1);
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (textField.mAutoCompletePanel.isShow()) {
                    textField.mAutoCompletePanel.selectFirst();
                } else {
                    return super.sendKeyEvent(event);
                }
                break;
            default:
                return super.sendKeyEvent(event);
        }
        return true;
    }

    /**
     * Only true when the InputConnection has not been used by the IME yet.
     * Can be programatically cleared by resetComposingState()
     */
    public boolean isComposingStarted() {
        return isComposing;
    }

    /**
     * 输入法传递过来的字符串
     *
     * @param text
     * @param newCursorPosition
     * @return
     */
    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        isComposing = true;
        if(!textField.hDoc.isBatchEdit()) {
            textField.hDoc.beginBatchEdit();
        }
        if (textField.mFieldController._isInSelectionMode) {
            textField.mFieldController.selectionDelete();
            composingCharCount = 0;
        }
        else {
            textField.mFieldController.replaceComposingText(
                    textField.getCaretPosition() - composingCharCount,
                    composingCharCount,
                    text.toString());
            composingCharCount = text.length();
        }
        //TODO reduce invalidate calls
        if(newCursorPosition > 1) {
            textField.mFieldController.moveCaret(mCaretPosition + newCursorPosition - 1);
        } else if(newCursorPosition <= 0) {
            textField.mFieldController.moveCaret(mCaretPosition - text.length() - newCursorPosition);
        }
        // log("setComposingText:"+text+","+newCursorPosition);
        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        log("commitText:"+text+","+newCursorPosition+","+composingCharCount);
             /*
            mFieldController.replaceComposingText(
                    getCaretPosition() - composingCharCount,
                    composingCharCount,
                    text.toString());
            composingCharCount = 0;
            hDoc.endBatchEdit();
            //TODO reduce invalidate calls
            if (newCursorPosition > 1) {
                mFieldController.moveCaret(mCaretPosition + newCursorPosition - 1);
            }
          else if(newCursorPosition==1){
              mFieldController.moveCaret(mCaretPosition + newCursorPosition);
        }
         //   else if (newCursorPosition <= 0) {
             //   mFieldController.moveCaret(mCaretPosition - text.length() - newCursorPosition);
            //}
            isComposing = false;
            return true;

              */
        return setComposingText(text, newCursorPosition) && finishComposingText();
    }


    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        if (composingCharCount != 0) {
            DLog.d("codeeditor","Warning: Implmentation of InputConnection.deleteSurroundingText" +
                    " will not skip composing text");
        }

        textField.mFieldController.deleteAroundComposingText(leftLength, rightLength);
        return true;
    }

    @Override
    public boolean finishComposingText() {
        resetComposingState();
        return true;
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        int capsMode = 0;

        // Ignore InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS; not used in TextWarrior

        if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
            int prevChar = mCaretPosition - 1;
            if (prevChar < 0 || Lexer.getLanguage().isWhitespace(textField.hDoc.charAt(prevChar))) {
                capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

                //set CAP_SENTENCES if client is interested in it
                if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                        == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }
        }

        // Strangely, Android soft keyboard does not set TYPE_TEXT_FLAG_CAP_SENTENCES
        // in reqModes even if it is interested in doing auto-capitalization.
        // Android bug? Therefore, we assume TYPE_TEXT_FLAG_CAP_SENTENCES
        // is always set to be on the safe side.
        else {
            Language lang = Lexer.getLanguage();

            int prevChar = mCaretPosition - 1;
            int whitespaceCount = 0;
            boolean capsOn = true;

            // Turn on caps mode only for the first char of a sentence.
            // A fresh line is also considered to start a new sentence.
            // The position immediately after a period is considered lower-case.
            // Examples: "abc.com" but "abc. Com"
            while (prevChar >= 0) {
                char c = textField.hDoc.charAt(prevChar);
                if (c == Language.NEWLINE) {
                    break;
                }

                if (!lang.isWhitespace(c)) {
                    if (whitespaceCount == 0 || !lang.isSentenceTerminator(c)) {
                        capsOn = false;
                    }
                    break;
                }

                ++whitespaceCount;
                --prevChar;
            }

            if (capsOn) {
                capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            }
        }

        return capsMode;
    }

    @Override
    public CharSequence getTextAfterCursor(int maxLen, int flags) {
        return textField.mFieldController.getTextAfterCursor(maxLen); //ignore flags
    }

    @Override
    public CharSequence getTextBeforeCursor(int maxLen, int flags) {
        return textField.mFieldController.getTextBeforeCursor(maxLen); //ignore flags
    }

    @Override
    public boolean setComposingRegion(int start, int end){
        isComposing = true;
        mCaretPosition = start;
        composingCharCount = end - start;
        return true;

    }
    @Override
    public boolean setSelection(int start, int end) {
        log("setSelection:"+start+","+end);

        if (start == end) {
            if (start == 0) {
                //适配搜狗输入法
                if (textField.getCaretPosition() > 0) {
                    textField.mFieldController.moveCaret(textField.getCaretPosition() - 1);
                }
            } else {
                textField.mFieldController.moveCaret(start);
            }
        } else {
            textField.mFieldController.setSelectionRange(start, end - start, false, true);
        }
        return true;
    }
}// end inner class
