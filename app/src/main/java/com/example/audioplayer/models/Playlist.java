package com.example.audioplayer.models;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private List<AudioTrack> tracks;

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
        this.tracks = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AudioTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<AudioTrack> tracks) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
    }

    public int getTrackCount() {
        return tracks != null ? tracks.size() : 0;
    }

    public String getFirstAlbumArtFileName() {
        if (tracks == null || tracks.isEmpty()) return null;
        return tracks.get(0).getAlbumArtFileName();
    }
}
