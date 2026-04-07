package com.example.audioplayer.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.Looper;

import com.example.audioplayer.models.AudioTrack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Mp3Scanner {
    private Context context;
    private OnScanCompleteListener listener;

    public interface OnScanCompleteListener {
        void onScanComplete(List<AudioTrack> tracks);
    }

    public Mp3Scanner(Context context,
                      OnScanCompleteListener listener){
        this.context = context;
        this.listener = listener;
    }

    public void startScanning(List<String> directories) {
        /*Начинаем сканирование в фоновом потоке и,
         получив данные, обновляем UI главного потока*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AudioTrack> foundTracks = new ArrayList<>();
                for(String dirPath : directories){
                    File dir = new File(dirPath);
                    if(dir.exists() && dir.isDirectory()){
                        scanDirectoryRecursive(dir, foundTracks);
                    }
                    //обработка ошибки нужна тост например
                }
                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run(){
                        if(listener != null){
                            listener.onScanComplete(foundTracks);
                        }
                    }
                });
            }
        }).start();
    }


}
