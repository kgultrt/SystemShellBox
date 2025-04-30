package com.manager.ssb.core.dialog;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.manager.ssb.R;

import java.util.Locale;

public class AudioPlayerDialog extends BottomSheetDialog {
    private MediaPlayer mediaPlayer;
    private final Handler progressHandler = new Handler();
    private boolean isPlaying = true;

    // UI
    private TextView tvFileName;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private Button btnPlayPause;
    private SeekBar seekBar;

    public AudioPlayerDialog(@NonNull Context context, String filePath, String fileName) {
        super(context);
        initView(context, fileName);
        initMediaPlayer(context, Uri.parse(filePath));
        startAudio();
    }

    private void initView(Context context, String fileName) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_audio_player, null);
        setContentView(view);

        tvFileName = view.findViewById(R.id.tv_file_name);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        seekBar = view.findViewById(R.id.seek_bar);

        tvFileName.setText(fileName);
        
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        
        btnPlayPause.setText(isPlaying ? R.string.dialog_pause : R.string.dialog_play);
    }

    private void initMediaPlayer(Context context, Uri audioUri) {
        mediaPlayer = MediaPlayer.create(context, audioUri);
        
        if (mediaPlayer != null) {
            // 设置播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setText(R.string.dialog_play);
                seekBar.setProgress(mp.getDuration());
                tvCurrentTime.setText(formatTime(mp.getDuration()));
                progressHandler.removeCallbacksAndMessages(null);
            });

            
            int duration = mediaPlayer.getDuration();
            tvTotalTime.setText(formatTime(duration));
            seekBar.setMax(duration);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }
    
    private void togglePlayPause() {
        if (isPlaying) {
            pauseAudio();
        } else {
            startAudio();
        }
    }

    private void startAudio() {
        if (mediaPlayer != null) {
            // 如果已播放完毕，重置到开头
            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                mediaPlayer.seekTo(0);
                seekBar.setProgress(0);
                tvCurrentTime.setText(formatTime(0));
            }
            
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setText(R.string.dialog_pause); // 文本切换
            updateProgress();
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setText(R.string.dialog_play); // 文本切换
        }
    }

    private void updateProgress() {
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    tvCurrentTime.setText(formatTime(currentPosition));
                    updateProgress();
                }
            }
        }, 10);
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        progressHandler.removeCallbacksAndMessages(null);
    }
}

