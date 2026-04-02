package com.example.audioplayer;

public class SelectedFolder {
    private String uriString;
    private String displayName;

    public SelectedFolder(String uriString, String displayName) {
        this.uriString = uriString;
        this.displayName = displayName;
    }

    public String getUriString() { return uriString; }
    public String getDisplayName() { return displayName; }
}