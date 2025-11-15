package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.StringRes;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Application extends android.app.Application {
    private static Application instance;

    public static Context getAppContext() {
        return instance;
    }
}