package com.example.audioplayer.services;

import static androidx.core.app.ServiceCompat.startForeground;
import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.audioplayer.R;
import com.example.audioplayer.TracksActivity;
import com.example.audioplayer.models.AudioTrack;

import java.io.IOException;

/**
 * Сервис для фоновоспроизведения аудио.
 * Реализует базовый функционал: загрузка трека, плей/пауза, отслеживание прогресса.
 * В будущем можно добавить: очередь треков, управление через MediaSession, уведомления.
 */



public class AudioPlayerService extends Service {
    private static final String TAG = "AudioPlayerService";
    private static final String CHANNEL_ID = "audio_player_channel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private AudioTrack currentTrack;
    private boolean isPrepared = false;

    public interface OnPlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdated(int currentPosition, int duration);
        void onTrackChanged(AudioTrack track);
    }
    private OnPlaybackListener playbackListener;

    /**
     * Binder для связи Activity с Service
     */
    public class LocalBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }
    public AudioTrack getCurrentTrack() {
        return currentTrack;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaPlayer = new MediaPlayer();
        setupMediaPlayerListeners();
    }

    /**
     * Настройка слушателей MediaPlayer
     */
    private void setupMediaPlayerListeners() {
        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            // Автозапуск после подготовки (опционально)
            // mediaPlayer.start();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            // Трек закончился — можно перейти к следующему
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Ошибка воспроизведения: what=" + what + ", extra=" + extra);
            return false;
        });
    }

    /**
     * Создание канала уведомлений для Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Управление воспроизведением музыки");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void loadTrack(AudioTrack track) {
        if (track == null || track.getFilePath() == null) {
            Log.e(TAG, "Некорректный трек");
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(track.getFilePath());
            mediaPlayer.prepareAsync();

            currentTrack = track;
            isPrepared = false;

            if (playbackListener != null) {
                playbackListener.onTrackChanged(track);
            }

            showNotification(track);

        }
        catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки трека: " + track.getFilePath(), e);
        }
    }

    /**
     * Показать уведомление с элементами управления
     */
    private void showNotification(AudioTrack track) {
        Intent notificationIntent = new Intent(this, TracksActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setSmallIcon(R.drawable.ic_delete)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }


    public void play() {
        if (isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            startProgressUpdates();
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(true);
            }
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            stopProgressUpdates();
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void seekTo(int position) {
        if (isPrepared) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }


    private Thread progressThread;
    private volatile boolean shouldUpdateProgress = false;

    private void startProgressUpdates() {
        shouldUpdateProgress = true;
        progressThread = new Thread(() -> {
            while (shouldUpdateProgress && isPrepared) {
                try {
                    Thread.sleep(200);
                    if (playbackListener != null) {
                        playbackListener.onProgressUpdated(
                                mediaPlayer.getCurrentPosition(),
                                mediaPlayer.getDuration()
                        );
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        progressThread.start();
    }

    private void stopProgressUpdates() {
        shouldUpdateProgress = false;
        if (progressThread != null) {
            progressThread.interrupt();
        }
    }

    public void setPlaybackListener(OnPlaybackListener listener) {
        this.playbackListener = listener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
