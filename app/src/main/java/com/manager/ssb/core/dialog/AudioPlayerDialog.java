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
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
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
import com.google.android.material.slider.Slider;
import com.manager.ssb.R;
import com.manager.ssb.core.FileTypeRegistry;

import com.manager.nativelayer.XmpPlayer;

import java.util.Locale;

public class AudioPlayerDialog {
    private final AlertDialog dialog;
    private MediaPlayer mediaPlayer;
    private XmpPlayer xmpPlayer;
    private AudioTrack audioTrack;
    private Thread trackerThread;
    private final Handler progressHandler = new Handler();
    private boolean isPlaying = true;
    private final boolean isTracker;

    // UI 控件
    private TextView tvFileName;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private Button btnPlayPause;
    private Slider slider;

    public AudioPlayerDialog(@NonNull Context context, String filePath, String fileName) {
        isTracker = isTrackerModule(filePath);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_audio_player, null);
        builder.setView(view);
        dialog = builder.create();

        dialog.setOnDismissListener(dialogInterface -> releasePlayer());
        dialog.setOnCancelListener(dialogInterface -> releasePlayer());

        initView(view, fileName);

        if (isTracker) {
            initTrackerPlayer(filePath);
        } else {
            initMediaPlayer(context, Uri.parse(filePath));
        }

        startAudio();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    // ============ 工具方法 ============

    private boolean isTrackerModule(String path) {
        String ext = FileTypeRegistry.getFileExtension(path);
        // 这里列出 xmp 常用支持的扩展名，可按需增减
        String[] trackerExts = {
                ".mod", ".xm", ".s3m", ".it", ".stm", ".far", ".669",
                ".mtm", ".ptm", ".ult", ".mdl", ".okt", ".stx", ".pt3",
                ".dbm", ".gdm", ".med", ".umx", ".abk", ".amf",
                ".digi", ".flt", ".fnk", ".ice", ".imf", ".ims",
                ".liq", ".masi", ".mfp", ".mgt", ".mmd1", ".mmd3",
                ".no", ".rtm", ".sfx", ".stim", ".sym", ".xmf",
                ".coco", ".dt", ".emod", ".gal4", ".gal5", ".hmn",
                ".masi16", ".mmd_common", ".pw", ".hrt", ".arch",
                ".noiserun", ".skyt", ".titanics", ".novotrade",
                ".ptp", ".p60a", ".p61a", ".p40", ".xann", ".zen"
        };
        for (String e : trackerExts) {
            if (e.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    // ============ 初始化 UI ============

    private void initView(View view, String fileName) {
        tvFileName = view.findViewById(R.id.tv_file_name);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        slider = view.findViewById(R.id.slider);

        tvFileName.setText(fileName);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlayPause.setText(isPlaying ? R.string.dialog_pause : R.string.dialog_play);
    }

    // ============ 普通音频播放（MediaPlayer）============

    private void initMediaPlayer(Context context, Uri audioUri) {
        mediaPlayer = MediaPlayer.create(context, audioUri);

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setText(R.string.dialog_play);
                int duration = mp.getDuration();
                slider.setValue(duration);
                tvCurrentTime.setText(formatTime(duration));
                progressHandler.removeCallbacksAndMessages(null);
            });

            int duration = mediaPlayer.getDuration();
            tvTotalTime.setText(formatTime(duration));

            slider.setValueFrom(0f);
            slider.setValueTo(duration);
            slider.setValue(0f);

            slider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo((int) value);
                }
            });
        }
    }

    private void startMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                mediaPlayer.seekTo(0);
                slider.setValue(0f);
                tvCurrentTime.setText(formatTime(0));
            }
            mediaPlayer.start();
            updateMediaProgress();
        }
    }

    private void pauseMediaPlayer() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void updateMediaProgress() {
        progressHandler.postDelayed(() -> {
            if (mediaPlayer != null && isPlaying && !isTracker) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                float clamped = Math.max(slider.getValueFrom(),
                        Math.min(slider.getValueTo(), (float) currentPosition));
                slider.setValue(clamped);
                tvCurrentTime.setText(formatTime(currentPosition));
                updateMediaProgress();
            }
        }, 10);
    }

    // ============ Tracker 播放（xmp）============

    private void initTrackerPlayer(String filePath) {
        xmpPlayer = new XmpPlayer();
        if (!xmpPlayer.nativeInit(filePath)) {
            // 加载失败，可以简单提示
            return;
        }

        int sampleRate = 44100;
        int bufSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(bufSize)
                .build();

        // 对于 tracker 没有固定总时长，这里做如下处理
        slider.setEnabled(false);           // 禁止拖动
        tvTotalTime.setText("∞");           // 显示无限
        slider.setValueFrom(0f);
        slider.setValueTo(100f);            // 不用更新
    }

    private void startTracker() {
        if (audioTrack == null || xmpPlayer == null) return;
        audioTrack.play();
        trackerThread = new Thread(() -> {
            int bufSize = AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            short[] buffer = new short[bufSize / 2];
            while (isPlaying && !Thread.currentThread().isInterrupted()) {
                int ret = xmpPlayer.nativeFillBuffer(buffer, buffer.length);
                if (ret != 0) break; // 播放结束或出错
                audioTrack.write(buffer, 0, buffer.length);
            }
            progressHandler.post(() -> {
                isPlaying = false;
                btnPlayPause.setText(R.string.dialog_play);
            });
        });
        trackerThread.start();
    }

    private void pauseTracker() {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.pause();
        }
        if (trackerThread != null) {
            trackerThread.interrupt();
        }
    }

    // ============ 通用播放控制 ============

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    private void togglePlayPause() {
        if (isPlaying) {
            pauseAudio();
        } else {
            startAudio();
        }
    }

    private void startAudio() {
        if (isTracker) {
            startTracker();
        } else {
            startMediaPlayer();
        }
        isPlaying = true;
        btnPlayPause.setText(R.string.dialog_pause);
    }

    private void pauseAudio() {
        if (isTracker) {
            pauseTracker();
        } else {
            pauseMediaPlayer();
        }
        isPlaying = false;
        btnPlayPause.setText(R.string.dialog_play);
    }

    // ============ 格式化时间 ============

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // ============ 资源释放 ============

    private void releasePlayer() {
        if (isTracker) {
            if (trackerThread != null) {
                trackerThread.interrupt();
                try {
                    trackerThread.join(200); // 等待线程退出
                } catch (InterruptedException ignored) { }
                trackerThread = null;
            }
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
            if (xmpPlayer != null) {
                xmpPlayer.nativeRelease();
                xmpPlayer = null;
            }
        } else {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
        progressHandler.removeCallbacksAndMessages(null);
    }
}