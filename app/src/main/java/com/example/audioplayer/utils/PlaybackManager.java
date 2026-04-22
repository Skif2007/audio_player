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

import java.util.ArrayList;
import java.util.List;

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

    // Слушатели для обновления UI в разных активностях (несколько одновременно)
    private final List<AudioPlayerService.OnPlaybackListener> uiListeners = new ArrayList<>();

    // Контекст плейлиста для автоперехода к следующему треку
    private String currentPlaylistId;
    private List<AudioTrack> currentPlaylistTracks;

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
            if (!uiListeners.isEmpty()) {
                playerService.setPlaybackListener(compositeListener);
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

    public void playTrackFromPlaylist(@NonNull AudioTrack track, @Nullable String playlistId, @Nullable List<AudioTrack> playlistTracks) {
        this.currentPlaylistId = playlistId;
        this.currentPlaylistTracks = playlistTracks != null ? new ArrayList<>(playlistTracks) : null;
        playTrack(track);
    }

    @Nullable
    public AudioTrack getNextTrackInPlaylist() {
        if (currentPlaylistTracks == null || currentPlaylistTracks.isEmpty()) return null;
        AudioTrack current = getCurrentTrack();
        if (current == null) return null;
        for (int i = 0; i < currentPlaylistTracks.size(); i++) {
            if (currentPlaylistTracks.get(i).getFilePath().equals(current.getFilePath())) {
                if (i < currentPlaylistTracks.size() - 1) {
                    return currentPlaylistTracks.get(i + 1);
                }
                break;
            }
        }
        return null;
    }

    @Nullable
    public String getCurrentPlaylistId() {
        return currentPlaylistId;
    }

    public void clearPlaylistContext() {
        currentPlaylistId = null;
        currentPlaylistTracks = null;
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

    public void addUiListener(@NonNull AudioPlayerService.OnPlaybackListener listener) {
        if (!uiListeners.contains(listener)) {
            uiListeners.add(listener);
        }
        if (playerService != null) {
            playerService.setPlaybackListener(compositeListener);
        }
    }

    public void removeUiListener(@NonNull AudioPlayerService.OnPlaybackListener listener) {
        uiListeners.remove(listener);
    }

    // Обратная совместимость — заменяет единственный слушатель
    public void setUiListener(@Nullable AudioPlayerService.OnPlaybackListener listener) {
        uiListeners.clear();
        if (listener != null) {
            uiListeners.add(listener);
        }
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

    // Composite listener — перенаправляет события всем зарегистрированным слушателям
    private final AudioPlayerService.OnPlaybackListener compositeListener = new AudioPlayerService.OnPlaybackListener() {
        @Override
        public void onPlaybackStateChanged(boolean isPlaying) {
            for (AudioPlayerService.OnPlaybackListener l : new ArrayList<>(uiListeners)) {
                l.onPlaybackStateChanged(isPlaying);
            }
        }

        @Override
        public void onProgressUpdated(int currentPosition, int duration) {
            for (AudioPlayerService.OnPlaybackListener l : new ArrayList<>(uiListeners)) {
                l.onProgressUpdated(currentPosition, duration);
            }
        }

        @Override
        public void onTrackChanged(AudioTrack track) {
            for (AudioPlayerService.OnPlaybackListener l : new ArrayList<>(uiListeners)) {
                l.onTrackChanged(track);
            }
        }

        @Override
        public void onTrackCompleted() {
            for (AudioPlayerService.OnPlaybackListener l : new ArrayList<>(uiListeners)) {
                l.onTrackCompleted();
            }
        }
    };
}
