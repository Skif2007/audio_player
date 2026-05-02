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
    private enum PlayerState { IDLE, PREPARING, READY, PLAYING, ERROR }
    private PlayerState playerState = PlayerState.IDLE;
    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private AudioTrack currentTrack;
    private boolean isPrepared = false;

    public interface OnPlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdated(int currentPosition, int duration);
        void onTrackChanged(AudioTrack track);
        void onTrackCompleted();
    }
    private OnPlaybackListener playbackListener;


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


    private void setupMediaPlayerListeners() {
        mediaPlayer.setOnPreparedListener(mp -> {
            if (playerState == PlayerState.PREPARING) {
                playerState = PlayerState.READY;
            }
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            playerState = PlayerState.IDLE;
//            isPrepared = false;
            stopProgressUpdates();
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
                playbackListener.onTrackCompleted();
            }
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what);
            playerState = PlayerState.ERROR;
            isPrepared = false;
            stopProgressUpdates();
            return true;
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
        if (track == null || track.getFilePath() == null) return;

        synchronized (this) {
            try {
                stopProgressUpdates();

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();

                isPrepared = false;
                playerState = PlayerState.PREPARING;
                currentTrack = track;

                mediaPlayer.setDataSource(track.getFilePath());
                mediaPlayer.prepare();

                isPrepared = true;
                playerState = PlayerState.READY;

                if (playbackListener != null) {
                    playbackListener.onTrackChanged(track);
                }
                showNotification(track);

            } catch (IllegalStateException | IOException e) {
                Log.e(TAG, "Failed to load track", e);
                playerState = PlayerState.ERROR;
                isPrepared = false;
                try { mediaPlayer.reset(); } catch (Exception ignored) {}
            }
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
        if (playerState == PlayerState.IDLE) {
            mediaPlayer.seekTo(0);
            playerState = PlayerState.READY;
        }
        if (!isPrepared || playerState != PlayerState.READY) {
            return;
        }
        synchronized (this) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                playerState = PlayerState.PLAYING;
                startProgressUpdates();
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(true);
                }
            }
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playerState = PlayerState.READY;
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
        stopProgressUpdates();

        shouldUpdateProgress = true;
        progressThread = new Thread(() -> {
            while (shouldUpdateProgress) {
                try {
                    Thread.sleep(200);

                    if (isPrepared && playerState == PlayerState.PLAYING) {
                        int pos = mediaPlayer.getCurrentPosition();
                        int dur = mediaPlayer.getDuration();

                        if (playbackListener != null && dur > 0) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                    playbackListener.onProgressUpdated(pos, dur)
                            );
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (IllegalStateException e) {
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
            try { progressThread.join(100); } catch (InterruptedException ignored) {}
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
