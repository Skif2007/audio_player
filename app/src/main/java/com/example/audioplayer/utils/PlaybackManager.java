package com.example.audioplayer.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.services.AudioPlayerService;

/**
 * Singleton-менеджер для централизованного управления воспроизведением.
 * Позволяет любой части приложения контролировать плеер без прямой привязки к Service.
 * В будущем: добавить очередь, shuffle, repeat, медиа-сессии.
 */

public class PlaybackManager {

    private static final String TAG = "PlaybackManager";
    private static PlaybackManager instance;

    private AudioPlayerService playerService;
    private boolean isBound = false;
    private Context appContext;
    private long lastPlayRequestTime = 0;
    private static final long MIN_PLAY_INTERVAL_MS = 150;

    // Слушатели для обновления UI в разных активностях
    private AudioPlayerService.OnPlaybackListener uiListener;

    private PlaybackManager() {}

    public static synchronized PlaybackManager getInstance() {
        if (instance == null) {
            instance = new PlaybackManager();
        }
        return instance;
    }


    public void init(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void bindService() {
        if (appContext == null) {
            Log.e(TAG, "PlaybackManager не инициализирован!");
            return;
        }
        Intent intent = new Intent(appContext, AudioPlayerService.class);
        appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindService() {
        if (isBound && appContext != null) {
            appContext.unbindService(serviceConnection);
            isBound = false;
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            playerService = binder.getService();
            isBound = true;
            if (uiListener != null) {
                playerService.setPlaybackListener(uiListener);
            }
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
            isBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    public void playTrack(@NonNull AudioTrack track) {
        if (playerService == null) {
            bindService();
            return;
        }
        AudioTrack current = playerService.getCurrentTrack();
        if (current != null &&
                current.getFilePath() != null &&
                current.getFilePath().equals(track.getFilePath()) &&
                playerService.isPlaying()) {
            return;
        }

        playerService.loadTrack(track);
        playerService.play();
    }

    public void togglePlayback() {
        if (playerService == null) return;
        if (playerService.isPlaying()) {
            playerService.pause();
        } else {
            playerService.play();
        }
    }

    public boolean isPlaying() {
        return playerService != null && playerService.isPlaying();
    }


    @Nullable
    public AudioTrack getCurrentTrack() {
        return playerService != null ? playerService.getCurrentTrack() : null;
    }

    public void setUiListener(@Nullable AudioPlayerService.OnPlaybackListener listener) {
        this.uiListener = listener;
        if (playerService != null) {
            playerService.setPlaybackListener(listener);
        }
    }

    @Nullable
    public AudioPlayerService getService() {
        return playerService;
    }

    public boolean isReady() {
        return isBound && playerService != null;
    }
}
