package com.manager.ssb.core;

import com.manager.ssb.core.openmethod.AudioFileHandler;
import com.manager.ssb.core.openmethod.CompressFileHandler;
import com.manager.ssb.core.openmethod.HtmlFileHandler;
import com.manager.ssb.core.openmethod.TextFileHandler;

public class HandlerInitializer {
    public static void init() {
        HandlerRegistry.register(FileType.AUDIO, new AudioFileHandler());
        HandlerRegistry.register(FileType.TEXT, new TextFileHandler());
        HandlerRegistry.register(FileType.COMPRESS, new CompressFileHandler());
        HandlerRegistry.register(FileType.HTML, new HtmlFileHandler());
    }
}