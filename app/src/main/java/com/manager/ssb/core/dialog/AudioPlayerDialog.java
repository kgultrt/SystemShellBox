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

package com.manager.ssb.core.dialog;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider; // 导入 Slider
import com.manager.ssb.R;

import java.util.Locale;

public class AudioPlayerDialog {
    private final AlertDialog dialog;
    private MediaPlayer mediaPlayer;
    private final Handler progressHandler = new Handler();
    private boolean isPlaying = true;

    // UI 控件
    private TextView tvFileName;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private Button btnPlayPause;
    private Slider slider; // 替换原来的 SeekBar

    public AudioPlayerDialog(@NonNull Context context, String filePath, String fileName) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_audio_player, null);
        builder.setView(view);
        dialog = builder.create();

        dialog.setOnDismissListener(dialogInterface -> releaseMediaPlayer());
        dialog.setOnCancelListener(dialogInterface -> releaseMediaPlayer());

        initView(view, fileName);
        initMediaPlayer(context, Uri.parse(filePath));
        startAudio();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    private void initView(View view, String fileName) {
        tvFileName = view.findViewById(R.id.tv_file_name);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        slider = view.findViewById(R.id.slider); // 初始化 Slider

        tvFileName.setText(fileName);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlayPause.setText(isPlaying ? R.string.dialog_pause : R.string.dialog_play);
    }

    private void initMediaPlayer(Context context, Uri audioUri) {
        mediaPlayer = MediaPlayer.create(context, audioUri);

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setText(R.string.dialog_play);
                // 播放完成后滑块停留在末尾，更新时间
                int duration = mp.getDuration();
                slider.setValue(duration);
                tvCurrentTime.setText(formatTime(duration));
                progressHandler.removeCallbacksAndMessages(null);
            });

            int duration = mediaPlayer.getDuration();
            tvTotalTime.setText(formatTime(duration));

            // 设置 Slider 的范围（从 0 到 音频总时长，单位毫秒）
            slider.setValueFrom(0f);
            slider.setValueTo(duration);
            slider.setValue(0f);

            // 添加 Slider 的监听器（代替 SeekBar 的 setOnSeekBarChangeListener）
            slider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && mediaPlayer != null) {
                    // 将滑块的 float 值转为 int 并跳转
                    mediaPlayer.seekTo((int) value);
                }
            });
        }
    }

    private void togglePlayPause() {
        if (isPlaying) pauseAudio();
        else startAudio();
    }

    private void startAudio() {
        if (mediaPlayer != null) {
            // 如果已播放到结尾，重置到开头
            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                mediaPlayer.seekTo(0);
                slider.setValue(0f);
                tvCurrentTime.setText(formatTime(0));
            }

            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setText(R.string.dialog_pause);
            updateProgress();
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setText(R.string.dialog_play);
        }
    }

    private void updateProgress() {
        progressHandler.postDelayed(() -> {
            if (mediaPlayer != null && isPlaying) {
                int currentPosition = mediaPlayer.getCurrentPosition();

                // 钳制到合法范围
                float clamped = Math.max(slider.getValueFrom(),
                        Math.min(slider.getValueTo(), (float) currentPosition));
                slider.setValue(clamped);

                tvCurrentTime.setText(formatTime(currentPosition));
                updateProgress();
            }
        }, 10);
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        progressHandler.removeCallbacksAndMessages(null);
    }
}