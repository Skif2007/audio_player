package com.example.audioplayer.models;

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
