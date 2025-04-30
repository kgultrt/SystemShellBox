// UnknownFileHandler.java (具体的文件处理器)
package com.manager.ssb.core.openmethod;

import android.content.Context;
import android.widget.Toast;

import com.manager.ssb.core.FileHandler;

public class UnknownFileHandler implements FileHandler {
    @Override
    public void handle(Context context, String filePath, String fileName) {
        Toast.makeText(context, "Unsupported file type", Toast.LENGTH_SHORT).show();
    }
}