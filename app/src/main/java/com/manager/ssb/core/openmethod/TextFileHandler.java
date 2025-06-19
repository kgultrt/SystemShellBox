// TextFileHandler.java (具体的文件处理器)
package com.manager.ssb.core.openmethod;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.manager.ssb.core.FileHandler;
import com.manager.ssb.core.editor.TextEditorActivity;

public class TextFileHandler implements FileHandler {
    @Override
    public void handle(Context context, String filePath, String fileName) {
        //Toast.makeText(context, "Opening text file: " + fileName, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(context, TextEditorActivity.class);
        intent.putExtra("file_path", filePath);
        intent.putExtra("file_name", fileName);
        context.startActivity(intent);
    }
}