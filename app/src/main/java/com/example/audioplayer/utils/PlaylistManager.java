package com.example.audioplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.models.Playlist;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaylistManager {

    private static final String PREFS_NAME = "playlist_settings";
    private static final String KEY_PLAYLISTS = "playlists_json";

    private static PlaylistManager instance;
    private final List<Playlist> playlists;

    private PlaylistManager() {
        this.playlists = new ArrayList<>();
    }

    public static synchronized PlaylistManager getInstance() {
        if (instance == null) {
            instance = new PlaylistManager();
        }
        return instance;
    }

    public Playlist createPlaylist(String name) {
        String id = UUID.randomUUID().toString();
        Playlist playlist = new Playlist(id, name);
        playlists.add(playlist);
        return playlist;
    }

    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists);
    }

    public Playlist getPlaylist(String id) {
        for (Playlist p : playlists) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public void deletePlaylist(String id) {
        playlists.removeIf(p -> p.getId().equals(id));
    }

    public void addTrackToPlaylist(String playlistId, AudioTrack track) {
        Playlist playlist = getPlaylist(playlistId);
        if (playlist != null && track != null) {
            playlist.getTracks().add(track);
        }
    }

    public void removeTrackFromPlaylist(String playlistId, int position) {
        Playlist playlist = getPlaylist(playlistId);
        if (playlist != null && position >= 0 && position < playlist.getTracks().size()) {
            playlist.getTracks().remove(position);
        }
    }

    public void moveTrackInPlaylist(String playlistId, int fromPosition, int toPosition) {
        Playlist playlist = getPlaylist(playlistId);
        if (playlist == null) return;
        List<AudioTrack> tracks = playlist.getTracks();
        if (fromPosition < 0 || fromPosition >= tracks.size()) return;
        if (toPosition < 0 || toPosition >= tracks.size()) return;
        if (fromPosition == toPosition) return;
        AudioTrack track = tracks.remove(fromPosition);
        tracks.add(toPosition, track);
    }

    public void saveToPrefs(Context context) {
        Gson gson = new Gson();
        String json = gson.toJson(playlists);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PLAYLISTS, json).apply();
    }

    public void loadFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PLAYLISTS, null);
        if (json == null || json.isEmpty()) {
            playlists.clear();
            return;
        }
        Gson gson = new Gson();
        List<Playlist> loaded = gson.fromJson(json, new TypeToken<List<Playlist>>() {}.getType());
        playlists.clear();
        if (loaded != null) {
            playlists.addAll(loaded);
        }
    }

    public void removeTrackFromPlaylist(String playlistId, String filePath) {
        Playlist playlist = getPlaylist(playlistId);
        if (playlist != null && filePath != null) {
            playlist.getTracks().removeIf(t -> t.getFilePath() != null && t.getFilePath().equals(filePath));
        }
    }

    public void removeTrackFromAllPlaylists(String filePath) {
        if (filePath == null) return;
        for (Playlist p : playlists) {
            p.getTracks().removeIf(t -> t.getFilePath() != null && t.getFilePath().equals(filePath));
        }
    }
}
