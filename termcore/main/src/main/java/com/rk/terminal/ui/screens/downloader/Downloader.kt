package com.rk.terminal.ui.screens.downloader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.ui.res.stringResource
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.Rootfs
import com.rk.terminal.ui.screens.terminal.TerminalScreen

import com.rk.terminal.R

import java.io.File

@Composable
fun Downloader(
    modifier: Modifier = Modifier,
    mainActivity: MainActivity,
    navController: NavHostController
) {
    val requiredFiles = listOf(
        File(Rootfs.reTerminal, "proot"),
        File(Rootfs.reTerminal, "libtalloc.so.2")
    )

    val allFilesExist = requiredFiles.all { it.exists() }

    LaunchedEffect(allFilesExist) {
        if (allFilesExist) {
            navController.navigate("terminal")
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!allFilesExist) {
           Text("Error: DNITE", style = MaterialTheme.typography.bodyLarge)
        }
    }
}