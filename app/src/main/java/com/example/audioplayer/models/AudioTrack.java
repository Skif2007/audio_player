package com.example.audioplayer.models;

import java.io.File;

public class AudioTrack {
    private String filePath;
    private String title;
    private String artist;
    private String albumArtFileName;

    public AudioTrack(String filePath, String title,
                      String artist, String albumArtUri){
        this.filePath = filePath;
        this.title = title;
        this.artist = artist;
        this.albumArtFileName = albumArtUri;
    }

    public AudioTrack() { }

    public String getTitle(){
        return title;
    }
    public String getFilePath(){
        return filePath;
    }
    public String getArtist(){
        return artist;
    }
    public String getAlbumArtFileName(){
        return albumArtFileName;
    }



    public File getFile() {
        return filePath != null ? new File(filePath) : null;
    }
    public long getFileSize() {
        File f = getFile();
        return f != null && f.exists() ? f.length() : -1;
    }
    public String getFileFormat() {
        if (filePath == null) return "Неизвестно";
        String name = filePath.toLowerCase();
        if (name.endsWith(".mp3")) return "MP3";
        if (name.endsWith(".flac")) return "FLAC";
        if (name.endsWith(".wav")) return "WAV";
        if (name.endsWith(".ogg")) return "OGG";
        if (name.endsWith(".m4a")) return "M4A/AAC";
        return "Неизвестно";
    }


    public void setTitle(String title){
        this.title = title;
    }
    public void setArtist(String artist){
        this.artist = artist;
    }
    public void setFilePath(String filePath){
        this.filePath = filePath;
    }
    public void setAlbumArtFileName(String albumArtFileName){
        this.albumArtFileName = albumArtFileName;
    }

}
