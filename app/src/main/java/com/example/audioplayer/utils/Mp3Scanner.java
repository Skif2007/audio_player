package com.example.audioplayer.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.audioplayer.models.AudioTrack;

import java.io.File;
import java.io.FileOutputStream;
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

    private void scanDirectoryRecursive(File directory, List<AudioTrack> tracks){
        /*Метод для рекурсивного обхода всех подпапок и поиска*/
        File[] files = directory.listFiles();
        if(files == null) return;
        for(File file : files){
            if(file.isDirectory()){
                scanDirectoryRecursive(file, tracks);
            }
            else if(file.isFile() && file.getName().toLowerCase().endsWith(".mp3")){
                AudioTrack track = extractMetadata(file);
                if(track != null){
                    tracks.add(track);
                }
            }
        }
    }

    private AudioTrack extractMetadata(File file){
        /*Извлекаем метаданные из файла*/
        MediaMetadataRetriever retriver = new MediaMetadataRetriever();
        try{
            retriver.setDataSource(file.getAbsolutePath());
            String title = retriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if(title == null || title.isEmpty()){
                title = file.getName();
                if(title.endsWith(".mp3") || title.endsWith(".MP3")){
                    title = title.substring(0, title.length() - 4);
                }
            }
            String artist = retriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if(artist == null || artist.isEmpty()){
                artist = "Неизвестный исполнитель";
            }
            byte[] artBytes = retriver.getEmbeddedPicture();
            String artUri = null;
            if(artBytes != null){
                artUri = saveArtToInternalStorage(file.getName(), artBytes);
            }
            return new AudioTrack(file.getAbsolutePath(), title, artist, artUri);
        }
        catch (Exception e) {
            //трек будет пропущен, подумать нужно ли менять
            return null;
        }
        finally{
            // вот тут жалуется подправить
            retriver.release();
        }
    }

    private String saveArtToInternalStorage(String filename, byte[] artBytes){
        /*Сохраняем обложку во внутреннюю память приложения*/
        try {
            String artFileName = "cover_" + filename.hashCode() + ".jpg";
            FileOutputStream fos = context.openFileOutput(artFileName, Context.MODE_PRIVATE);
            fos.write(artBytes);
            fos.close();
            return Uri.fromFile(new File(context.getFilesDir(), artFileName)).toString();
        }
        catch(Exception e) {
            //Подумать может доработать
            return null;
        }
    }
}
