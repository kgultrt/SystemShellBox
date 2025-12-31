/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.apkpatch;

import android.app.Activity;
import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import com.manager.ssb.databinding.ActivityApkpatchBinding;
import java.io.IOException;

public class MainActivity extends Activity {

    private ActivityApkpatchBinding binding;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApkpatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化MediaPlayer并播放背景音乐（从Assets中的keygen/music.wav）
        playMusic();
    }
    
    public void playMusic() {
        try {
            AssetFileDescriptor afd = getAssets().openFd("keygen/music.mp3");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放MediaPlayer资源
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        this.binding = null;
    }
}
