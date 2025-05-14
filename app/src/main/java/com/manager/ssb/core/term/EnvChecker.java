// EnvChecker.java
package com.manager.ssb.core.term;

import android.content.Context;
import java.io.File;

public class EnvChecker {
    public static boolean isEnvironmentInstalled(Context context) {
        File shFile = new File(context.getFilesDir(), "usr/bin/sh");
        return shFile.exists();
    }
}