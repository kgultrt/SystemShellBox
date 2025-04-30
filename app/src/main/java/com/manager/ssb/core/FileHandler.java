// FileHandler.java (接口)
package com.manager.ssb.core;

import android.content.Context;

public interface FileHandler {
    void handle(Context context, String filePath, String fileName);
}