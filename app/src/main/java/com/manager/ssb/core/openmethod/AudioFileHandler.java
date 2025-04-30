// AudioFileHandler.java (具体的文件处理器)
package com.manager.ssb.core.openmethod;

import android.content.Context;

import com.manager.ssb.core.FileHandler;
import com.manager.ssb.core.dialog.AudioPlayerDialog;

public class AudioFileHandler implements FileHandler {
    @Override
    public void handle(Context context, String filePath, String fileName) {
        new AudioPlayerDialog(context, filePath, fileName).show();
    }
}