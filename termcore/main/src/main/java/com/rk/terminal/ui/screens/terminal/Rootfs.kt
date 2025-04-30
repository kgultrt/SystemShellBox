package com.rk.terminal.ui.screens.terminal

import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import com.rk.libcommons.application
import com.rk.libcommons.child
import java.io.File

object Rootfs {
    val reTerminal = File("/data/data/com.manager.ssb/files/usr")
    val binPath = File("/data/data/com.manager.ssb/files/usr/bin")
    val checkFile = File("/data/data/com.manager.ssb/files/usr/bin/sh")

    init {
        if (reTerminal.exists().not()){
            reTerminal.mkdirs()
        }
        
        if (binPath.exists().not()){
            binPath.mkdirs()
        }
    }

    var isDownloaded = mutableStateOf(isFilesDownloaded())
    fun isFilesDownloaded(): Boolean{
        return reTerminal.exists() && checkFile.exists()
    }
}