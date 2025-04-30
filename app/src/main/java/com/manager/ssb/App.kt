package com.manager.ssb

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import com.github.anrwatchdog.ANRWatchDog
import com.rk.libcommons.application
import com.rk.resources.Res
import com.rk.update.UpdateManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors


class App : Application() {

    companion object {
        // 静态 Context 引用
        private lateinit var appContext: Context
        
        fun getTempDir(): File {
            val tmp = File(application!!.filesDir.parentFile, "tmp")
            if (!tmp.exists()) {
                tmp.mkdir()
            }
            return tmp
        }
        
        @JvmStatic
        fun getAppContext(): Context = appContext
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        appContext = this // 初始化静态 Context
        application = this
        Res.application = this

        //Thread.setDefaultUncaughtExceptionHandler(CrashHandler)
        ANRWatchDog().start()

        UpdateManager().onUpdate()

        if (BuildConfig.DEBUG){
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectAll()
                    penaltyLog()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                        penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            println(violation.message)
                            violation.printStackTrace()
                            violation.cause?.let { throw it }
                            println("vm policy error")
                        }
                    }
                }.build()
            )
        }



        GlobalScope.launch(Dispatchers.IO) {
            getTempDir().apply {
                if (exists() && listFiles().isNullOrEmpty().not()){ deleteRecursively() }
            }
        }

    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

}